# app/services/animal_introduction.py
import asyncio
import logging
from langchain_openai import ChatOpenAI
from langchain.prompts import PromptTemplate
from fastapi import HTTPException
from typing import Tuple, List
from app.core.config import settings
from app.models.animal import Animal
from app.db.session import get_db_context
from app.db.session import get_db_context, get_background_db_context
from app.crud.animal import find_animals_by_ids, find_recent_animal_ids_with_null_title, update_animal_introduction
from app.utils.retry import with_db_retry
from app.utils.rate_limiting import RateLimiter

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

BATCH_SIZE = 30  # 메모리에 올라갈 배치 job 갯수
CONCURRENT_LIMIT = 10  # API 요청 병렬 처리량

class AnimalIntroductionService:

    def __init__(self, model_name: str = "gpt-4o-mini", temperature: float = 0.5, max_tokens: int = 600):
        self.model_name = model_name
        self.temperature = temperature
        self.max_tokens = max_tokens
        self.api_key = settings.OPENAI_API_KEY
        self.rate_limiter = RateLimiter(max_calls_per_period=600, period_seconds=60.0)

    async def update_introductions_batch(self, animal_ids: List[int]) -> None:
        total_animals = len(animal_ids)
        logger.info(f"총 {total_animals}개 동물 소개글 업데이트 작업 시작")
        
        # 배치 단위로 처리 => 메모리에 올라가는 task 제어와 DB에서 가져오는 동물 데이터 prefetch 목적으로 배치 처리 
        for i in range(0, total_animals, BATCH_SIZE):
            batch = animal_ids[i:i+BATCH_SIZE]
            batch_size = len(batch)
            logger.info(f"배치 처리 시작 ({i+1}-{i+batch_size}/{total_animals})")
            
            async with get_background_db_context() as db:
                animals = await find_animals_by_ids(db, batch)

            # 배치 내 모든 동물을 태스크로 변환
            tasks = [self._process_single_animal(animal) for animal in animals]
            
            # 동시성 제한을 위해 청크 단위로 태스크 실행
            for j in range(0, len(tasks), CONCURRENT_LIMIT):
                chunk = tasks[j:j+CONCURRENT_LIMIT]
                await asyncio.gather(*chunk) # 여러 태스크를 동시에 실행 (비동기 병렬 처리)
            
            logger.info(f"배치 처리 완료 - {batch_size}개 동물")

    async def _process_single_animal(self, animal: Animal) -> None:
        try:
            # 소개글이 없을 때만 생성
            if animal and not animal.introduction_title:
                title, intro = await self._generate_introduction(animal)

                async with get_background_db_context() as db:
                    await update_animal_introduction(db, animal.id, title, intro)
                logger.info(f"동물 ID {animal.id} 업데이트 완료.")
            else:
                logger.info(f"동물 ID {animal.id}는 이미 소개글이 있거나 존재하지 않습니다.")
        except Exception as e:
            logger.error(f"동물 ID {animal.id} 업데이트 실패: {str(e)}")

    async def _generate_introduction(self, animal: Animal) -> Tuple[str, str]:
        prompt = self._create_prompt(animal)
        response_text = await self._generate_text_with_llm(prompt)
        return self._parse_response(response_text, animal.id)

    def _create_prompt(self, animal) -> str:
        return (
        "### Persona ###\n"
        "1. Your task is to write a introduction for the animal based on the following information.\n"
        "2. Highlight the animal's positive traits and suitability as a pet to encourage potential adopters.\n"
        
        "### Animal Information ###\n"
        f"Name: {animal.name}\n"
        f"Species: {animal.kind}\n"
        f"Gender: {'Male' if animal.gender == 'M' else 'Female'}\n"
        f"Spayed/Neutered: {'Yes' if animal.neuter == 'Y' else 'No'}\n"
        f"Color: {animal.color}\n"
        f"Approximate Age: {animal.age}\n"
        f"Location Found: {animal.happen_place}\n"
        f"Special Characteristics: {animal.special_mark}\n"
        
        "### Background ###\n"
        f"{animal.name}, a {animal.kind}, was found in {animal.happen_place}. Known for its {animal.special_mark} and unique {animal.color} coat, {animal.name} has shown loving nature despite its circumstances.\n"
        
        "### Title ###\n"
        "1. Provide a normal title for the introduction in Korean.\n"
        "2. The title should be 25 characters or fewer.\n"
        "3. Please include only the city/district without the detailed address for {animal.happen_place}. {animal.happen_place}'에서 발견된' {animal.age}'살' {animal.name}.\n"
        
        "### Response Format ###\n"
        "1. The first line should be the title, prefixed with 'Title: '.\n"
        "2. After the title, there should be two newline characters (\\n\\n).\n"
        "3. The introduction should follow after the newline characters.\n"
        
        "### Writing Guidelines ###\n"
        "1. Provide the introduction in Korean.\n"
        "2. Write from the perspective of a shelter staff member who cares for the animal. Use a tone that conveys warmth and care. More than half of the sentences should end with '요' to maintain a gentle tone."
        "3. Keep the text between 150 and 230 characters and avoid using negative language. "
        "4. Avoid repetition.\n"
        "5. Include details about the animal's characteristics, gender, and approximate age throughout the description."
        )

    async def _generate_text_with_llm(self, prompt: str) -> str:
        try:
            # LLM API 호출 전 API 요청 LIMIT 체크 (2차 방어)
            await self.rate_limiter.acquire()

            chatmodel = ChatOpenAI(
                model=self.model_name,
                temperature=self.temperature,
                max_tokens=self.max_tokens,
                openai_api_key=self.api_key
            )
            
            prompt_template = PromptTemplate(input_variables=["prompt"], template="{prompt}")
            formatted_prompt = prompt_template.format(prompt=prompt)
            
            # LangChain 라이브러리에서 네이티브 비동기 호출 함수 지원하면 사용
            if hasattr(chatmodel, 'ainvoke'):
                response = await chatmodel.ainvoke(formatted_prompt)
            else:
                response = await asyncio.to_thread(chatmodel.invoke, formatted_prompt)
            
            return response.content
        except Exception as e:
            logger.error(f"LLM API error: {str(e)}")
            raise HTTPException(status_code=500, detail="텍스트 생성 중 오류가 발생했습니다.")

    def _parse_response(self, response_text: str, animal_id: int) -> Tuple[str, str]:
        if not response_text:
            logger.error(f"동물 ID {animal_id}에 대해 빈 응답이 반환되었습니다.")
            raise HTTPException(status_code=500, detail="생성된 소개글이 비어 있습니다.")
            
        response_lines = response_text.strip().split('\n')
        
        # 첫 번째 줄(제목) 이후부터 실제 내용이 시작되는 줄을 찾을 때까지 빈 줄을 무시
        if len(response_lines) < 2:
            logger.error(f"동물 ID {animal_id}에 대한 응답이 예상된 형식이 아닙니다: {response_text}")
            raise HTTPException(status_code=500, detail="생성된 소개글 형식이 잘못되었습니다.")
            
        title = response_lines[0].replace("Title: ", "").strip()

        content_start_index = 1
        while content_start_index < len(response_lines) and not response_lines[content_start_index].strip():
            content_start_index += 1

        introduction = '\n'.join(response_lines[content_start_index:]).strip()
        
        return title, introduction

# 의존성 주입 제공자
def get_introduction_service() -> AnimalIntroductionService:
    return AnimalIntroductionService()

async def update_animal_introductions(animal_ids, service: AnimalIntroductionService):
    try:
        await service.update_introductions_batch(animal_ids)
    except Exception as e:
        logger.error(f"동물 소개글 업데이트 중 오류 발생: {str(e)}")
    finally:
        logger.info(f"{len(animal_ids)}개 동물 소개글 업데이트 작업 완료")

@with_db_retry(max_retries=3)
async def find_animals_without_introduction():
    async with get_db_context() as db:
        animal_ids = await find_recent_animal_ids_with_null_title(db)
    return animal_ids