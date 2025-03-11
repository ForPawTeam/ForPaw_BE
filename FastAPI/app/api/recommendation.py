# app/api/recommendation.py
import math
from typing import List
from fastapi import APIRouter
from app.schemas.cf import InteractionDTO
from fastapi import BackgroundTasks
from app.services.cb import update_animal_similarity_data
from app.services.cf import (train_cf_model, build_cf_recommendations, store_cf_results_in_redis)

router = APIRouter()

ALPHA = 1.0    # 좋아요 가중치
BETA = 0.2     # 조회수 가중치
GAMMA = 2.0    # 문의 가중치

@router.post("/cb/update/similarity")
async def update_similarity(background_tasks: BackgroundTasks):
    """
    동물 유사도 데이터를 업데이트
    """
    # 백그라운드 작업으로 처리하여 응답 지연 방지
    background_tasks.add_task(update_animal_similarity_data, top_k=5)
    return {
        "status": "success", 
        "message": "동물 유사도 데이터 업데이트 작업이 백그라운드에서 시작되었습니다."
    }

@router.post("/cf/import")
async def import_interactions(interactions: List[InteractionDTO]):
    """
    스프링에서 전송한 사용자-동물 상호작용 데이터를 처리하여 협업 필터링 모델 학습 및 추천 결과 생성
    
    작동 과정
    1. 각 상호작용을 가중치를 적용한 단일 점수로 변환
    2. 변환된 데이터로 협업 필터링 모델 학습
    3. 각 사용자에 대한 추천 결과 생성 및 Redis에 저장
    """
    # 상호작용 데이터에 가중치 적용하여 단일 점수로 변환
    computed_interactions = []
    for inter in interactions:
        # 가중 합산 => (좋아요 × ALPHA) + (log(조회수+1) × BETA) + (문의 수 × GAMMA)
        # log 변환은 조회수가 많더라도 점수가 지나치게 커지는 것을 방지
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
    
    user_ids = list({entry["user_id"] for entry in computed_interactions})
    animal_ids = list({entry["animal_id"] for entry in computed_interactions})
    
    # 각 사용자에 대해 상위 top_n 추천 동물 목록  생성
    cf_results = await build_cf_recommendations(algo, user_ids, animal_ids, top_n=5)
    
    await store_cf_results_in_redis(cf_results)