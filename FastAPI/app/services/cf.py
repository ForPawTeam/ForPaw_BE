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
    """
    특정 사용자에 대한 협업 필터링 추천 결과를 Redis에서 가져옴

    Returns
        추천 동물 ID와 정규화된 점수를 포함하는 맵 {animal_id: score}
        - 점수는 순위에 기반하며, 상위 항목일수록 높은 점수 (1.0~0.0)
    """
    # Redis에서 추천 리스트 가져오기
    key = f"CF:{user_id}"
    cf_candidates_str = await redis_client.lrange(key, 0, -1)
    cf_scores = {}

    if cf_candidates_str:
        n = len(cf_candidates_str)
        for i, cand_str in enumerate(cf_candidates_str):
            try:
                candidate = int(cand_str)
                score = (n - i) / n  # 순위에 따른 점수 부여 -> 첫 번째는 1.0, 마지막은 1/n
                cf_scores[candidate] = score
            except ValueError:
                continue
    return cf_scores

async def train_cf_model(interactions: List[Dict[str, Any]]):
    """
    사용자-동물 상호작용 데이터를 기반으로 협업 필터링 모델 학습
    
    작동 원리
    1. 상호작용 데이터를 판다스 DataFrame으로 변환
    2. 평점을 0-10 범위로 정규화
    3. Surprise 라이브러리를 사용해 SVD 모델 학습
    4. 테스트 세트로 모델 성능 평가 (RMSE)
    """
    if not interactions:
        logger.warning("사용자-동물 상호작용 데이터가 없습니다.")
        return None

    # DataFrame 생성
    df = pd.DataFrame(interactions, columns=["user_id", "animal_id", "rating"])

    # 정규화 by min-max scaling (0~10 범위)
    # SVD 알고리즘은 일반적으로 일정 범위의 평점을 기대하므로 정규화 필요
    min_rating = df["rating"].min()
    max_rating = df["rating"].max()
    df["rating_norm"] = (df["rating"] - min_rating) / (max_rating - min_rating) * 10

    # 정규화된 rating 값을 사용해서 train set 생성
    reader = Reader(rating_scale=(0, 10))
    data = Dataset.load_from_df(df[["user_id", "animal_id", "rating_norm"]], reader)

    # 학습/테스트 데이터 분할 (80%/20%)
    trainset, testset = train_test_split(data, test_size=0.2, random_state=42)

    # SVD 모델 학습 (하이퍼파라미터 튜닝 적용)
    # n_factors: 잠재 요인 수 (사용자와 동물을 표현하는 벡터 차원)
    # n_epochs: 학습 반복 횟수
    # lr_all: 학습률
    # reg_all: 정규화 계수 (과적합 방지)
    algo = SVD(n_factors=50, n_epochs=30, lr_all=0.005, reg_all=0.02)
    algo.fit(trainset)

    # 검증 및 RMSE 측정 (낮을수록 좋음)
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
    """
    학습된 SVD 모델을 사용하여 각 사용자별 추천 목록 생성
    
    작동 원리
    1. 각 사용자에 대해 모든 동물의 예상 평점 계산
    2. 평점이 높은 순으로 정렬하여 상위 top_n개 동물 ID 선택
    
    Args
        algo: 학습된 SVD 모델
        user_ids: 추천을 생성할 사용자 ID 목록
        all_animal_ids: 전체 동물 ID 목록
        top_n: 사용자별 추천할 동물 수
    """
    recommendations = {}
    for uid in user_ids:
        score_list = []

        # 해당 사용자에 대해 모든 동물의 예상 평점 계산
        for aid in all_animal_ids:
            # Surprise는 문자열 형태의 ID를 사용하므로 변환
            pred = algo.predict(str(uid), str(aid))
            score_list.append((aid, pred.est))

        # 예측 평점 내림차순 정렬
        score_list.sort(key=lambda x: x[1], reverse=True)
        # 상위 top_n개 동물 ID만 추출
        top_animals = [a for (a, score) in score_list[:top_n]]
        recommendations[uid] = top_animals

    return recommendations

async def store_cf_results_in_redis(cf_results: Dict[int, List[int]]):   
    """
    협업 필터링 추천 결과를 Redis에 저장
    
    저장 형식
    - 키: "CF:{user_id}"
    - 값: 추천 동물 ID 리스트 (순서 중요)
    """ 
    for user_id, animal_ids in cf_results.items():
        key = f"CF:{user_id}"
        await redis_client.delete(key)
        if animal_ids:
            await redis_client.rpush(key, *map(str, animal_ids))