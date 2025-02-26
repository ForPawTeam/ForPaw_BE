# main.py
import asyncio
import random
from fastapi import FastAPI, BackgroundTasks
from contextlib import asynccontextmanager
from apscheduler.schedulers.asyncio import AsyncIOScheduler

from .schemas import RecommendRequest, AnimalIntroductionRequest
from .services import (
    redis_client,
    update_animal_similarity_data,
    get_similar_animals_from_redis,
    generate_animal_introduction,
    find_animals_without_introduction,
    update_animal_introductions
)

app = FastAPI()

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 앱 시작 시, 전체 TF-IDF 유사도 계산 & Redis 저장
    await update_animal_similarity_data(top_k=5)

    scheduler = AsyncIOScheduler()
    scheduler.add_job(
        lambda: asyncio.run(update_animal_similarity_data(top_k=5)),
        'cron',
        hour=1
    )
    scheduler.start()

    try:
        yield
    finally:
        scheduler.shutdown()

app.router.lifespan_context = lifespan

@app.post("/recommend/animal")
async def recommend_animal(request: RecommendRequest):
    # user_id 기반으로 Redis에서 유저가 본 동물 ID 조회
    key = f"animal:search:{request.user_id}"
    animal_ids_str = redis_client.lrange(key, 0, -1)
    if not animal_ids_str:
        return {"recommendedAnimals": []}
    
    animal_ids = list(map(int, animal_ids_str))

    unique_ids = set()
    for animal_id in animal_ids:
        similar_ids = await get_similar_animals_from_redis(animal_id)
        unique_ids.update(similar_ids)

    unique_list = list(unique_ids)
    if len(unique_list) > 5:
        recommended_animals = random.sample(unique_list, 5)
    else:
        recommended_animals = unique_list

    return {"recommendedAnimals": recommended_animals}

@app.post("/introduce/animal")
async def process_animal_introduction(background_tasks: BackgroundTasks):
    await update_animal_similarity_data(top_k=5)

    # 소개문이 없는 동물 조회 후, background task로 처리
    animal_ids = await find_animals_without_introduction()
    background_tasks.add_task(update_animal_introductions, animal_ids)
    
    return {"success": "true", "code": 200, "message": "OK", "result": "null"}

@app.post("/introduce/animal/test")
async def introduce_animal(request: AnimalIntroductionRequest):
    introduction = await generate_animal_introduction(request.animal_id)
    
    return {"introduction": introduction}