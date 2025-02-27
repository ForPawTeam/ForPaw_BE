# app/api/animal.py
from fastapi import APIRouter, BackgroundTasks
from app.schemas.animal import RecommendRequest, AnimalIntroductionRequest
from app.services.animal import generate_animal_introduction, find_animals_without_introduction, update_animal_introductions, hybrid_recommendation
from app.services.cb import update_animal_similarity_data
from app.utils.sync_tasks import sync_update_animal_introductions

router = APIRouter()

@router.post("/recommend/animal")
async def recommend_animal_hybrid(request: RecommendRequest):
    recommended = await hybrid_recommendation(request.user_id, final_k=5)

    return {"recommendedAnimals": recommended}

@router.post("/introduce/animal")
async def process_animal_introduction(background_tasks: BackgroundTasks):
    await update_animal_similarity_data(top_k=5)

    # 소개문이 없는 동물 조회 후, background task로 처리
    animal_ids = await find_animals_without_introduction()
    background_tasks.add_task(sync_update_animal_introductions, animal_ids)
    
    return {"success": "true", "code": 200, "message": "OK", "result": "null"}

@router.post("/introduce/animal/test")
async def introduce_animal(request: AnimalIntroductionRequest):
    introduction = await generate_animal_introduction(request.animal_id)
    
    return {"introduction": introduction}