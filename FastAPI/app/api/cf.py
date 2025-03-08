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
    # 상호작용 데이터에 가중치 적용하여 단일 점수로 변환
    computed_interactions = []

    # 가중 합산 => (좋아요 × ALPHA) + (log(조회수+1) × BETA) + (문의 수 × GAMMA)
    for inter in interactions:
        rating = ALPHA * inter.like_count + BETA * math.log(1 + inter.view_count) + GAMMA * inter.inquiry_count
        computed_interactions.append({
            "user_id": inter.user_id,
            "animal_id": inter.animal_id,
            "rating": rating
        })
    
    # 가중치가 적용된 상호작용 데이터로 협업 필터링 모델 학습
    algo = await train_cf_model(computed_interactions)
    if algo is None:
        return
    
    # 데이터에서 고유 사용자 ID와 동물 ID 추출
    user_ids = list({entry["user_id"] for entry in computed_interactions})
    animal_ids = list({entry["animal_id"] for entry in computed_interactions})
    
    # 각 사용자에 대해 상위 top_n 추천 동물 목록  생성
    cf_results = await build_cf_recommendations(algo, user_ids, animal_ids, top_n=5)
    
    # 생성된 추천 결과를 Redis에 저장
    await store_cf_results_in_redis(cf_results)