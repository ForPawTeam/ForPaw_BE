# app/api/cf.py
import math
from typing import List
from fastapi import APIRouter
from app.schemas.cf import InteractionDTO
from app.services.cf import (train_cf_model, build_cf_recommendations, store_cf_results_in_redis)

router = APIRouter()

ALPHA = 1.0    # 좋아요 가중치
BETA = 0.2     # 조회수 가중치
GAMMA = 2.0    # 문의 가중치

@router.post("/cf/import")
async def import_interactions(interactions: List[InteractionDTO]):
    computed_interactions = []
    for inter in interactions:
        rating = ALPHA * inter.like_count + BETA * math.log(1 + inter.view_count) + GAMMA * inter.inquiry_count
        computed_interactions.append({
            "user_id": inter.user_id,
            "animal_id": inter.animal_id,
            "rating": rating
        })
    
    # CF 모델 훈련
    algo = await train_cf_model(computed_interactions)
    if algo is None:
        return
    
    user_ids = list({entry["user_id"] for entry in computed_interactions})
    animal_ids = list({entry["animal_id"] for entry in computed_interactions})
    
    # 각 사용자에 대해 상위 top_n 추천
    cf_results = await build_cf_recommendations(algo, user_ids, animal_ids, top_n=5)
    
    await store_cf_results_in_redis(cf_results)