# app/services/animal_introduction.py
import logging
from langchain_openai import ChatOpenAI
from langchain.prompts import PromptTemplate
from fastapi import HTTPException
from typing import Tuple, List

from app.core.config import settings
from app.db.session import get_db_context
from app.crud.animal import find_animal_by_id, find_recent_animal_ids_with_null_title

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

BATCH_SIZE = 5  # 배치 처리 크기

class AnimalIntroductionService:

    def __init__(self, model_name: str = "gpt-4o-mini", temperature: float = 0.5, max_tokens: int = 750):
        self.model_name = model_name
        self.temperature = temperature
        self.max_tokens = max_tokens
        self.api_key = settings.OPENAI_API_KEY

    def _create_prompt(self, animal) -> str:
        return (
            "### Persona ###\n"
            "1. You are a compassionate shelter staff member writing an engaging introduction for an animal seeking adoption.\n"
            "2. Your goal is to create an emotional connection between potential adopters and the animal.\n"
            
            "### Animal Information ###\n"
            f"Name: {animal.name}\n"
            f"Species: {animal.kind}\n"
            f"Gender: {'Male' if animal.gender == 'M' else 'Female'}\n"
            f"Spayed/Neutered: {'Yes' if animal.neuter == 'Y' else 'No'}\n"
            f"Color: {animal.color}\n"
            f"Approximate Age: {animal.age}\n"
            f"Location Found: {animal.happen_place}\n"
            f"Special Characteristics: {animal.special_mark}\n"
            
            "### Personality Profile ###\n"
            "1. Describe 2-3 endearing personality traits (playful, gentle, loyal, etc.) that make this animal special.\n"
            "2. Mention how the animal interacts with humans (enjoys being petted, follows people, etc.).\n"
            "3. If applicable, describe how the animal gets along with other animals or children.\n"
            
            "### Story Elements ###\n"
            f"1. {animal.name} was found in {animal.happen_place} and has shown remarkable resilience.\n"
            "2. Include a short heartwarming anecdote about a positive interaction with shelter staff.\n"
            "3. Paint a picture of how this animal could enrich someone's home and daily life.\n"
            
            "### Title ###\n"
            "1. Create an attention-grabbing, heartwarming title in Korean (25 characters or fewer).\n"
            f"2. Format suggestion: {animal.happen_place}'에서 발견된' {animal.age}'살' {animal.name} - [unique trait].\n"
            "3. Include only the city/district without detailed address.\n"
            
            "### Response Format ###\n"
            "1. First line: Title prefixed with 'Title: '\n"
            "2. Two newline characters (\\n\\n)\n"
            "3. Introduction text\n"
            "4. Final line: Brief, warm call to action encouraging adoption inquiry\n"
            
            "### Writing Guidelines ###\n"
            "1. Provide the introduction in Korean.\n"
            "2. Write from the perspective of a shelter staff member who cares for the animal. Use a tone that conveys warmth and care. More than half of the sentences should end with '요' to maintain a gentle tone."
            "3. Keep the text between 150 and 230 characters and avoid using negative language. "
            "4. Avoid repetition.\n"
            "5. Include details about the animal's characteristics, gender, and approximate age throughout the description."
        )

    async def _generate_text_with_llm(self, prompt: str) -> str:
        try:
            chatmodel = ChatOpenAI(
                model=self.model_name,
                temperature=self.temperature,
                max_tokens=self.max_tokens,
                openai_api_key=self.api_key
            )
            
            prompt_template = PromptTemplate(input_variables=["prompt"], template="{prompt}")
            formatted_prompt = prompt_template.format(prompt=prompt)
            
            response = chatmodel.invoke(formatted_prompt)
            return response.content
        except Exception as e:
            logger.error(f"LLM API error: {str(e)}")
            raise HTTPException(status_code=500, detail="텍스트 생성 중 오류가 발생했습니다.")

    def _parse_response(self, response_text: str, animal_id: int) -> Tuple[str, str]:
        if not response_text:
            logger.error(f"동물 ID {animal_id}에 대해 빈 응답이 반환되었습니다.")
            raise HTTPException(status_code=500, detail="생성된 소개글이 비어 있습니다.")
            
        response_lines = response_text.strip().split('\n')
        
        if len(response_lines) < 2:
            logger.error(f"동물 ID {animal_id}에 대한 응답이 예상된 형식이 아닙니다: {response_text}")
            raise HTTPException(status_code=500, detail="생성된 소개글 형식이 잘못되었습니다.")
            
        title = response_lines[0].replace("Title: ", "").strip()
        introduction = '\n'.join(response_lines[1:]).strip()
        
        return title, introduction

    async def generate_introduction(self, animal_id: int) -> Tuple[str, str]:
        async with get_db_context() as db:
            animal = await find_animal_by_id(db, animal_id)
            if not animal:
                logger.error(f"동물 ID {animal_id}에 해당하는 동물이 존재하지 않습니다.")
                raise HTTPException(status_code=404, detail="해당 동물을 찾을 수 없습니다.")
            
            prompt = self._create_prompt(animal)
            response_text = await self._generate_text_with_llm(prompt)
            
            logger.info(f"동물 ID {animal_id}에 대해 생성된 응답 일부: {response_text[:50]}...")
            
            return self._parse_response(response_text, animal_id)
    
    async def update_introductions_batch(self, animal_ids: List[int]) -> None:
        async with get_db_context() as db:
            for i in range(0, len(animal_ids), BATCH_SIZE):  # 상수 사용
                batch = animal_ids[i:i+BATCH_SIZE]
                for animal_id in batch:
                    try:
                        title, introduction = await self.generate_introduction(animal_id)
                        animal = await find_animal_by_id(db, animal_id)

                        if animal:
                            animal.introduction_title = title
                            animal.introduction_content = introduction
                            db.add(animal)
                            logger.info(f"동물 ID {animal_id} 업데이트 완료.")
                    except Exception as e:
                        logger.error(f"동물 ID {animal_id} 업데이트 실패: {str(e)}")
                        # 배치의 나머지는 계속 처리

                await db.commit()
                logger.info(f"배치 업데이트(commit) 완료 - 동물 ID 배치: {batch}")

# 의존성 주입 제공자
def get_introduction_service() -> AnimalIntroductionService:
    return AnimalIntroductionService()

async def update_animal_introductions(animal_ids, service: AnimalIntroductionService):
    await service.update_introductions_batch(animal_ids)

async def find_animals_without_introduction():
    async with get_db_context() as db:
        animal_ids = await find_recent_animal_ids_with_null_title(db)
    return animal_ids