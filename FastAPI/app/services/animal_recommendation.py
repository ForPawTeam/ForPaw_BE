# app/services/animal_recommendation.py
import logging
from typing import List
from app.services import cf
from app.services import cb

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

async def hybrid_recommendation(
        user_id: int, 
        final_k: int = 5, 
        w_cf: float = 0.6, 
        w_cb: float = 0.4, 
        cb_top_k: int = 5
) -> List[int]:
    """
    작동 원리
    1. 협업 필터링과 콘텐츠 기반 필터링에서 각각 추천 후보와 점수 조회
    2. 두 점수를 설정된 가중치로 합산하여 최종 점수 계산
    3. 최종 점수가 높은 순으로 상위 final_k개 동물 ID 반환
    
    Args
        user_id: 추천을 받을 사용자 ID
        final_k: 최종 추천 수
        w_cf: 협업 필터링 가중치 (0~1)
        w_cb: 콘텐츠 기반 필터링 가중치 (0~1)
        cb_top_k: 콘텐츠 기반 필터링에서 고려할 유사 동물 수
    """
    # 협업 필터링 추천 후보 및 점수 조회
    cf_scores = await cf.get_cf_candidates(user_id)

    # 콘텐츠 기반 필터링 추천 후보 및 점수 조회
    cb_scores = await cb.get_cb_candidates(user_id, cb_top_k=cb_top_k)

    # 두 점수를 가중합하여 최종 점수 계산
    all_candidates = set(cf_scores.keys()).union(set(cb_scores.keys()))
    final_candidates = []
    
    # 각 후보에 대해 CF와 CB 점수를 가중합하여 최종 점수 계산
    for candidate in all_candidates:
        score_cf = cf_scores.get(candidate, 0)
        score_cb = cb_scores.get(candidate, 0)
        final_score = w_cf * score_cf + w_cb * score_cb
        final_candidates.append((candidate, final_score))

    # 최종 점수로 정렬하여 상위 final_k개 선택
    final_candidates.sort(key=lambda x: x[1], reverse=True)
    top_candidates = [candidate for candidate, score in final_candidates[:final_k]]

    logger.info("추천된 동물 ID: %s", top_candidates)
    
    return top_candidates