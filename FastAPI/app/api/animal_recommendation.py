# app/api/animal_recommendation.py
import math
from typing import List
from fastapi import APIRouter
from app.schemas.cf import InteractionDTO
from fastapi import BackgroundTasks
from app.schemas.animal import RecommendRequest
from app.services.cb import update_animal_similarity_data
from app.services.cf import process_interactions
from app.services.animal_recommendation import hybrid_recommendation

router = APIRouter()

@router.post("/recommend/animal")
async def recommend_animal_hybrid(request: RecommendRequest):
    recommended = await hybrid_recommendation(request.user_id, final_k=5)
    return {"recommendedAnimals": recommended}

@router.post("/cb/update/similarity")
async def update_similarity(background_tasks: BackgroundTasks):
    background_tasks.add_task(update_animal_similarity_data, top_k=5)
    return {
        "status": "success", 
        "message": "동물 유사도 데이터 업데이트 작업이 백그라운드에서 시작되었습니다."
    }

@router.post("/cf/update/interactions")
async def update_interactions(interactions: List[InteractionDTO], background_tasks: BackgroundTasks):
    background_tasks.add_task(process_interactions, interactions)
    return {
        "status": "success",
        "message": f"{len(interactions)}개 상호작용 데이터 처리가 백그라운드에서 시작되었습니다."
    }