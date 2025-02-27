# services/cb.py
import numpy as np  
import logging
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from app.core.config import settings
from app.db.session import get_db_session
from app.crud.animal import find_all_animals
from typing import List, Tuple
from redis.asyncio import Redis

logger = logging.getLogger(__name__)
vectorizer = TfidfVectorizer()
redis_client = None

async def init_redis():
    global redis_client
    redis_client = Redis(
        host=settings.REDIS_HOST,
        port=settings.REDIS_PORT,
        db=settings.REDIS_DB,
        decode_responses=True
    )

async def get_cb_candidates(user_id: int, cb_top_k: int = 5) -> dict:
    search_key = f"animal:search:{user_id}"
    searched_animals_str = await redis_client.lrange(search_key, 0, -1)
    cb_scores = {}

    if searched_animals_str:
        searched_animals = list(map(int, searched_animals_str))
        # 각 검색된 동물에 대해 유사 동물 목록을 조회하고 점수를 부여
        for animal_id in searched_animals:
            similar_ids = await _get_similar_animals(animal_id)
            if similar_ids:
                for i, sim_id in enumerate(similar_ids[:cb_top_k]):
                    score = (cb_top_k - i) / cb_top_k
                    cb_scores[sim_id] = cb_scores.get(sim_id, 0) + score
    return cb_scores

async def _get_similar_animals(animal_id: int) -> list:
    key = f"similar:{animal_id}"
    similar_ids_str = await redis_client.lrange(key, 0, -1)
    if not similar_ids_str:
        return []
    return list(map(int, similar_ids_str))

async def update_animal_similarity_data(top_k: int = 5):
    async for db in get_db_session():
        animals = await find_all_animals(db)

    if not animals:
        logger.warning("동물 데이터가 없습니다.")
        return

    # 동물 데이터를 기반으로 TF-IDF를 위한 텍스트와 ID 리스트 생성
    texts, animal_ids = _prepare_texts_and_ids(animals)

    # TF-IDF 벡터화 및 코사인 유사도 계산
    tfidf_matrix = vectorizer.fit_transform(texts)  # (N, D)
    sim_matrix = cosine_similarity(tfidf_matrix, tfidf_matrix)

    # 각 동물별 상위 top_k 유사 동물 ID를 Redis에 저장
    await _store_similar_animals_in_redis(animal_ids, sim_matrix, top_k)

    logger.info(f"동물별 상위 {top_k} 유사 동물 정보를 Redis에 저장 완료.")

def _prepare_texts_and_ids(animals) -> Tuple[List[str], List[int]]:
    texts = [
        f"{a.shelter_id} {a.age} {a.color} {a.gender} {a.kind} {a.region} {a.neuter}"
        for a in animals
    ]
    animal_ids = [a.id for a in animals]
    return texts, animal_ids

async def _store_similar_animals_in_redis(animal_ids: list, sim_matrix, top_k: int):
    for i, animal_id in enumerate(animal_ids):
        similarities = sim_matrix[i]

        # 내림차순 정렬 후 자기 자신은 제외
        sorted_indices = [idx for idx in np.argsort(similarities)[::-1] if idx != i]
        top_indices = sorted_indices[:top_k]
        top_similar_ids = [animal_ids[idx] for idx in top_indices]

        key = f"similar:{animal_id}"
        await redis_client.delete(key)
        if top_similar_ids:
            await redis_client.rpush(key, *top_similar_ids)