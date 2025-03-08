# app/services/animal_recommendation.py
import logging
from typing import List, Dict, Tuple
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