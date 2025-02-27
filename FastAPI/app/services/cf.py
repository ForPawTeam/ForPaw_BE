# services/cf.py
import logging
import pandas as pd
from redis.asyncio import Redis
from surprise import Dataset, Reader, SVD
from surprise import accuracy
from surprise.model_selection import train_test_split
from typing import List, Dict, Any
from app.core.config import settings

logger = logging.getLogger(__name__)
redis_client = None

async def init_redis():
    global redis_client
    redis_client = Redis(
        host=settings.REDIS_HOST,
        port=settings.REDIS_PORT,
        db=settings.REDIS_DB,
        decode_responses=True
    )

async def get_cf_candidates(user_id: int) -> Dict[int, float]:
    key = f"CF:{user_id}"
    cf_candidates_str = await redis_client.lrange(key, 0, -1)
    cf_scores = {}
    if cf_candidates_str:
        n = len(cf_candidates_str)
        for i, cand_str in enumerate(cf_candidates_str):
            try:
                candidate = int(cand_str)
                score = (n - i) / n  # 순위에 따른 점수 부여
                cf_scores[candidate] = score
            except ValueError:
                continue
    return cf_scores

async def train_cf_model(interactions: List[Dict[str, Any]]):
    if not interactions:
        logger.warning("사용자-동물 상호작용 데이터가 없습니다.")
        return None

    # DataFrame 생성
    df = pd.DataFrame(interactions, columns=["user_id", "animal_id", "rating"])

    # 정규화 by min-max scaling (0~10 범위)
    min_rating = df["rating"].min()
    max_rating = df["rating"].max()
    df["rating_norm"] = (df["rating"] - min_rating) / (max_rating - min_rating) * 10

    # 정규화된 rating 값을 사용해서 train set 생성
    reader = Reader(rating_scale=(0, 10))
    data = Dataset.load_from_df(df[["user_id", "animal_id", "rating_norm"]], reader)
    trainset, testset = train_test_split(data, test_size=0.2, random_state=42)

    # SVD 모델 학습 (하이퍼파라미터 튜닝 적용)
    algo = SVD(n_factors=50, n_epochs=30, lr_all=0.005, reg_all=0.02)
    algo.fit(trainset)

    # 검증 및 RMSE 측정
    predictions = algo.test(testset)
    rmse = accuracy.rmse(predictions, verbose=False)
    logger.info(f"[협업 필터링] SVD 모델 RMSE: {rmse:.4f}")

    return algo

async def build_cf_recommendations(
    algo,
    user_ids: List[int],
    all_animal_ids: List[int],
    top_n: int = 5
) -> Dict[int, List[int]]:
    
    recommendations = {}
    for uid in user_ids:
        score_list = []

        # user_id와 animal_id를 문자열로 변환하여 예측
        for aid in all_animal_ids:
            pred = algo.predict(str(uid), str(aid))
            score_list.append((aid, pred.est))

        # 예측 평점 내림차순 정렬
        score_list.sort(key=lambda x: x[1], reverse=True)
        top_animals = [a for (a, score) in score_list[:top_n]]
        recommendations[uid] = top_animals

    return recommendations

async def store_cf_results_in_redis(cf_results: Dict[int, List[int]]):    
    for user_id, animal_ids in cf_results.items():
        key = f"CF:{user_id}"
        await redis_client.delete(key)
        if animal_ids:
            await redis_client.rpush(key, *map(str, animal_ids))