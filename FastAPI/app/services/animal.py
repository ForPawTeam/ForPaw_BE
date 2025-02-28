# app/services/animal.py
import logging
from langchain_openai import ChatOpenAI
from langchain.prompts import PromptTemplate
from fastapi import HTTPException
from typing import List

from app.services import cf
from app.services import cb
from app.core.config import settings
from app.db.session import get_db_session
from app.crud.animal import find_animal_by_id, find_recent_animal_ids_with_null_title

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

async def hybrid_recommendation(
        user_id: int, 
        final_k: int = 5, 
        w_cf: float = 0.6, 
        w_cb: float = 0.4, 
        cb_top_k: int = 5
) -> List[int]:
    
    cf_scores = await cf.get_cf_candidates(user_id)
    cb_scores = await cb.get_cb_candidates(user_id, cb_top_k=cb_top_k)

    # 두 점수를 가중합하여 최종 점수 계산
    all_candidates = set(cf_scores.keys()).union(set(cb_scores.keys()))
    final_candidates = []
    for candidate in all_candidates:
        score_cf = cf_scores.get(candidate, 0)
        score_cb = cb_scores.get(candidate, 0)
        final_score = w_cf * score_cf + w_cb * score_cb
        final_candidates.append((candidate, final_score))

    final_candidates.sort(key=lambda x: x[1], reverse=True)
    top_candidates = [candidate for candidate, score in final_candidates[:final_k]]
    return top_candidates

async def generate_animal_introduction(animal_id):
    async with get_db_session() as db:
        animal = await find_animal_by_id(db, animal_id)
        if not animal:
            logger.error(f"동물 ID {animal_id}에 해당하는 동물이 존재하지 않습니다.")
            raise HTTPException(status_code=404, detail="해당 동물을 찾을 수 없습니다.")
    
    prompt = (
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

    chatmodel = ChatOpenAI(
        model="gpt-4o-mini", 
        temperature=0.3, 
        max_tokens=750, 
        openai_api_key = settings.OPENAI_API_KEY
        )
    
    prompt_template = PromptTemplate(input_variables=["prompt"], template="{prompt}")
    formatted_prompt = prompt_template.format(prompt=prompt)

    # response.content 사용하여 메시지 내용 추출
    response = chatmodel.invoke(formatted_prompt)
    response_text = response.content 
    logger.info(f"동물 ID {animal_id}에 대해 생성된 응답 일부: {response_text[:50]}...")
    
    # 타이틀만 추출
    response_lines = response_text.strip().split('\n')
    title = response_lines[0].replace("Title: ", "").strip()
    introduction = '\n'.join(response_lines[1:]).strip()

    if not response_lines:
        logger.error(f"동물 ID {animal_id}에 대해 빈 응답이 반환되었습니다.")

    return title, introduction

async def update_animal_introductions(animal_ids):
    # 5개씩 작업하고 커밋. (모두 처리하고 커밋하면, 에러가 발생하면 받아온 데이터 다 날릴 수 있음)
    async with get_db_session() as db:
        for i in range(0, len(animal_ids), 5):
            batch = animal_ids[i:i+5]
            for animal_id in batch:
                title, introduction = await generate_animal_introduction(animal_id)
                animal = await find_animal_by_id(db, animal_id)

                if animal:
                    animal.introduction_title = title
                    animal.introduction_content = introduction
                    db.add(animal)
                    logger.info(f"동물 ID {animal_id} 업데이트 완료.")

            await db.commit()
            logger.info(f"배치 업데이트(commit) 완료 - 동물 ID 배치: {batch}")

async def find_animals_without_introduction():
    async with get_db_session() as db:
        animal_ids = await find_recent_animal_ids_with_null_title(db)
    return animal_ids