# app/services/animal.py
import numpy as np  
import logging
from langchain_openai import ChatOpenAI
from langchain.prompts import PromptTemplate
from sklearn.feature_extraction.text import TfidfVectorizer
from fastapi import HTTPException
from typing import List
from sklearn.metrics.pairwise import cosine_similarity

from app.core.config import settings
from app.db.session import get_db_session
from app.crud.animal import find_animal_by_id, find_all_animals, find_animal_ids_with_null_title

# 전역으로 TF-IDF 벡터라이저를 정의
vectorizer = TfidfVectorizer()

import redis
redis_client = redis.Redis(
    host=settings.REDIS_HOST,
    port=settings.REDIS_PORT,
    db=settings.REDIS_DB,
    decode_responses=True
)

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

async def update_animal_similarity_data(top_k: int = 5):
    async with get_db_session() as db:
        animals = await find_all_animals(db)

    if not animals:
        logger.warning("동물 데이터가 없습니다.")
        return

    # 1) TF-IDF 위한 문자열 생성 
    texts = []
    animal_ids = []
    for a in animals:
        text = f"{a.shelter_id} {a.age} {a.color} {a.gender} {a.kind} {a.region} {a.neuter}"
        texts.append(text)
        animal_ids.append(a.id)

    # 2) TF-IDF 벡터화
    tfidf_matrix = vectorizer.fit_transform(texts)  # (N, D)

    # 3) 코사인 유사도 행렬 (N x N)
    sim_matrix = cosine_similarity(tfidf_matrix, tfidf_matrix)

    # 4) 각 동물별로 상위 top_k를 구해 Redis 저장
    for i, animal_id in enumerate(animal_ids):
        similarities = sim_matrix[i]
        sorted_indices = np.argsort(similarities)[::-1]   # 내림차순
        sorted_indices = [idx for idx in sorted_indices if idx != i]  # 자기 자신 제외
        top_indices = sorted_indices[:top_k]
        top_similar_ids = [animal_ids[idx] for idx in top_indices]

        # Redis key = "similar:{animal_id}"
        key = f"similar:{animal_id}"
        redis_client.delete(key)
        if top_similar_ids:
            redis_client.rpush(key, *top_similar_ids)

    logger.info(f"동물별 상위 {top_k} 유사 동물 정보를 Redis에 저장 완료.")

async def get_similar_animals_from_redis(animal_id: int) -> List[int]:
    key = f"similar:{animal_id}"
    similar_ids_str = redis_client.lrange(key, 0, -1)  # 문자열 리스트

    if not similar_ids_str:
        return []
    return list(map(int, similar_ids_str))

async def find_animals_without_introduction():
    async with get_db_session() as db:
        animal_ids = await find_animal_ids_with_null_title(db)
    return animal_ids

async def generate_animal_introduction(animal_id):
    async with get_db_session() as db:
        animal = await find_animal_by_id(db, animal_id)
        if not animal:
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
    
    # 타이틀만 추출
    response_lines = response_text.strip().split('\n')
    title = response_lines[0].replace("Title: ", "").strip()
    introduction = '\n'.join(response_lines[1:]).strip()

    return title, introduction

async def get_animal_ids_with_null_title():
    async with get_db_session() as db:
        return await find_animal_ids_with_null_title(db)

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
    
    logger.info("----------------배치 작업 완료----------------")

async def schedule_process_animal_introduction():
    animal_ids = await get_animal_ids_with_null_title()
    update_animal_introductions(animal_ids)