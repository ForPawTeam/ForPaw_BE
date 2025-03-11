# services/cb.py
import numpy as np  
import logging
import re
from datetime import datetime
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from app.core.config import settings
from app.db.session import get_background_db_context
from app.crud.animal import find_all_animals
from typing import List, Tuple, Dict, Any
from redis.asyncio import Redis

logger = logging.getLogger(__name__)
vectorizer = TfidfVectorizer()
redis_client = None

FEATURE_WEIGHTS = {
    'kind': 3.0,        # 동물 종은 매우 중요한 요소
    'age': 1.5,         # 나이는 중간 정도 중요
    'color': 1.2,       # 색상은 덜 중요
    'gender': 1.0,      # 성별은 덜 중요
    'region': 0.5,      # 지역은 크게 중요하지 않음
    'shelter_id': 0.8,  # 보호소는 어느 정도 중요
    'neuter': 0.8,      # 중성화 여부
    'special_mark': 1.0 # 특징은 중간 정도 중요
}

async def init_redis():
    global redis_client
    redis_client = Redis(
        host=settings.REDIS_HOST,
        port=settings.REDIS_PORT,
        db=settings.REDIS_DB,
        decode_responses=True
    )

async def get_cb_candidates(user_id: int, cb_top_k: int = 5) -> dict:
    """
    특정 사용자를 위한 콘텐츠 기반 추천 후보 목록과 점수 반환
    
    동작 원리
    1. 사용자가 검색한 동물 목록을 Redis에서 가져옴
    2. 각 검색 동물에 대해 유사한 동물 목록을 가져옴
    3. 유사한 동물에 순위 기반 점수 부여 (상위 항목일수록 높은 점수)
    4. 여러 검색 결과에서 나온 동물들의 점수를 합산
        
    Returns:
        유사 동물 ID와 점수를 포함하는 맵 {animal_id: score}
    """
    # Redis에서 사용자가 검색한 동물 ID 목록 조회
    search_key = f"animal:search:{user_id}"
    searched_animals_str = await redis_client.lrange(search_key, 0, -1)
    cb_scores = {}

    if searched_animals_str:
        searched_animals = list(map(int, searched_animals_str))

        # 각 검색된 동물에 대해 유사 동물 목록을 조회하고 점수를 부여
        for animal_id in searched_animals:
            similar_ids = await _get_similar_animals(animal_id)
            if similar_ids:
                # 상위 cb_top_k개 동물만 고려하여 점수 부여
                # 0부터 시작하는 인덱스이므로 순위는 i+1이 되어야 하지만,
                # 내림차순 점수를 위해 (cb_top_k - i) / cb_top_k 공식 사용
                for i, sim_id in enumerate(similar_ids[:cb_top_k]):
                    score = (cb_top_k - i) / cb_top_k  # 순위 기반 정규화 점수 (1.0 ~ 0.2)
                    cb_scores[sim_id] = cb_scores.get(sim_id, 0) + score  # 기존 점수에 누적
    return cb_scores

async def _get_similar_animals(animal_id: int) -> list:
    """
    Redis에서 특정 동물과 유사한 동물 ID 목록을 조회
    """
    key = f"similar:{animal_id}"
    similar_ids_str = await redis_client.lrange(key, 0, -1)
    if not similar_ids_str:
        return []
    return list(map(int, similar_ids_str))

async def update_animal_similarity_data(top_k: int = 5):
    """
    모든 동물 데이터에 대해 TF-IDF 기반 유사도를 계산하고 Redis에 저장
    
    동작 원리
    1. 데이터베이스에서 모든 동물 정보 조회
    2. 동물 특성(보호소, 나이, 색상 등)을 문자열로 변환
    3. TF-IDF 벡터화 및 코사인 유사도 계산
    4. 각 동물마다 유사도 높은 top_k개 동물을 Redis에 저장
    
    Args
        top_k: 각 동물마다 저장할 유사 동물 수
    """
    try:
        # 컨텍스트 매니저를 사용하여 세션 자동 관리
        async with get_background_db_context() as db:
            logger.info("동물 데이터 조회 중...")
            animals = await find_all_animals(db)

            if not animals:
                logger.warning("동물 데이터가 없습니다.")
                return

            # 동물 데이터 전처리 및 특성 추출
            logger.info(f"{len(animals)}개 동물 데이터 전처리 중...")
            animal_features, animal_ids = _prepare_animal_features(animals)

            # 특성 가중치 적용 및 유사도 계산
            logger.info("유사도 계산 중...")
            sim_matrix = _compute_similarity(animal_features)

            # 각 동물별 상위 top_k 유사 동물 ID를 Redis에 저장
            logger.info("Redis에 유사도 데이터 저장 중...")
            await _store_similar_animals_in_redis(animal_ids, sim_matrix, top_k)

            logger.info(f"동물별 상위 {top_k} 유사 동물 정보를 Redis에 저장 완료.")
    
    except Exception as e:
        logger.error(f"동물 유사도 데이터 업데이트 중 오류 발생: {str(e)}")
        raise

def _prepare_animal_features(animals) -> Tuple[List[Dict[str, Any]], List[int]]:
    animal_features = []
    animal_ids = []
    
    for animal in animals:
        features = {
            'text_features': _create_text_features(animal),
            
            'categorical': {
                'gender': animal.gender if animal.gender else 'U',
                'neuter': animal.neuter if animal.neuter else 'U',
                'process_state': animal.process_state if animal.process_state else 'unknown',
                'region': animal.region if animal.region else 'unknown',
                'age_category': _normalize_age(animal.age),         
                'weight_category': _normalize_weight(animal.weight)
            },
            
            'numerical': {
                'age_years': _extract_age_years(animal.age),      
                'weight_kg': _extract_weight_kg(animal.weight)    
            }
        }
        
        animal_features.append(features)
        animal_ids.append(animal.id)
    
    return animal_features, animal_ids

def _create_text_features(animal) -> str:
    """
    동물의 텍스트 특성을 가중치를 적용하여 문자열로 결합 => 가중치가 높은 특성은 여러 번 반복하여 중요도를 높임
    """
    features = []
    
    # 특성별 가중치에 따라 반복 횟수 결정
    kind_repeat = int(FEATURE_WEIGHTS['kind'] * 3)
    color_repeat = int(FEATURE_WEIGHTS['color'] * 3)
    special_mark_repeat = int(FEATURE_WEIGHTS['special_mark'] * 3)
    
    # 각 특성을 가중치에 비례하여 반복
    if animal.kind:
        features.extend([animal.kind] * kind_repeat)
    
    if animal.color:
        features.extend([animal.color] * color_repeat)
    
    if animal.special_mark:
        features.extend([animal.special_mark] * special_mark_repeat)
    
    # 기타 텍스트 특성 추가
    if animal.region:
        features.append(animal.region)
        
    if hasattr(animal, 'shelter_id') and animal.shelter_id:
        features.append(str(animal.shelter_id))
    
    # 공백으로 구분된 문자열로 결합
    return ' '.join(features)

def _extract_age_years(age_str) -> float:
    """
    나이 문자열에서 연도 추출하여 숫자로 변환
    """
    if not age_str:
        return -1  # 알 수 없는 경우
    
    try:
        # 연도 추출 (YYYY 형식)
        year_match = re.search(r'(\d{4})', age_str)
        if year_match:
            birth_year = int(year_match.group(1))
            current_year = datetime.now().year
            age_years = current_year - birth_year
            return float(age_years)
    except Exception as e:
        logger.warning(f"나이 추출 오류 ('{age_str}'): {str(e)}")
    
    return -1  # 알 수 없는 경우

def _extract_weight_kg(weight_str) -> float:
    """
    체중 문자열에서 kg 값 추출
    """
    if not weight_str:
        return -1  # 알 수 없는 경우
    
    try:
        # 숫자 패턴 추출
        weight_match = re.search(r'(\d+[,.]?\d*)', weight_str)
        if weight_match:
            # 쉼표를 점으로 변환 (0,2 -> 0.2)
            weight_str = weight_match.group(1).replace(',', '.')
            return float(weight_str)
    except Exception as e:
        logger.warning(f"체중 추출 오류 ('{weight_str}'): {str(e)}")
    
    return -1  # 알 수 없는 경우

def _normalize_weight(weight_str) -> str:
    """
    체중 문자열을 범주형 값으로 정규화
    """
    weight_kg = _extract_weight_kg(weight_str)
    
    if weight_kg < 0:
        return "unknown_weight"
    
    # 체중 범주화
    if weight_kg < 1:
        return "very_small"  # 1kg 미만 (아기 고양이, 작은 토끼 등)
    elif weight_kg <= 5:
        return "small"       # 소형 (고양이, 소형견 등)
    elif weight_kg <= 15:
        return "medium"      # 중형 (중형견 등)
    elif weight_kg <= 30:
        return "large"       # 대형 (대형견 등)
    else:
        return "very_large"  # 30kg 초과 (초대형견 등)
    
def _normalize_age(age_str) -> str:
    """
    나이 문자열을 범주형 값으로 정규화
    """
    age_years = _extract_age_years(age_str)
    
    if age_years < 0:
        return "unknown_age"
    
    # 연령대 범주화
    if age_years <= 1:
        return "puppy_kitten"  # 강아지/새끼고양이 (0-1세)
    elif age_years <= 3:
        return "young"         # 어린 동물 (1-3세)
    elif age_years <= 7:
        return "adult"         # 성체 (3-7세)
    elif age_years <= 10:
        return "senior"        # 노년기 초반 (7-10세)
    else:
        return "very_senior"   # 노년기 후반 (10세 이상)

def _compute_similarity(features) -> np.ndarray:
    """
    여러 유형의 특성을 고려한 가중치가 적용된 유사도 계산
    
    텍스트 특성, 범주형 특성, 수치형 특성을 각각 처리하고
    가중치를 적용하여 최종 유사도 행렬 생성
    
    Returns
        유사도 행렬 (NxN) - 각 동물 쌍 간의 유사도를 포함
    """
    n_samples = len(features)
    
    # 1. 텍스트 특성 유사도 계산 (TF-IDF + 코사인 유사도)
    text_features = [f['text_features'] for f in features]
    vectorizer = TfidfVectorizer(min_df=1, max_df=0.9)
    
    # 텍스트 데이터가 비어있을 경우 처리
    if not any(text_features):
        logger.warning("텍스트 특성이 비어 있습니다.")
        text_sim = np.ones((n_samples, n_samples))
    else:
        tfidf_matrix = vectorizer.fit_transform(text_features)
        text_sim = cosine_similarity(tfidf_matrix, tfidf_matrix)
    
    # 2. 범주형 특성 유사도 계산 (일치 여부 기반)
    cat_sim = np.ones((n_samples, n_samples))
    
    cat_weights = {
        'gender': 1.0,           # 성별
        'neuter': 0.8,           # 중성화 여부
        'process_state': 0.5,    # 처리 상태
        'region': 0.5,           # 지역
        'age_category': 1.5,     # 나이 범주 (더 중요)
        'weight_category': 1.2   # 체중 범주 (중요)
    }
    
    for i in range(n_samples):
        for j in range(i+1, n_samples):
            # 각 범주형 특성별 일치 여부 확인
            cat_i = features[i]['categorical']
            cat_j = features[j]['categorical']
            
            # 가중치를 적용한 일치 점수 계산
            weighted_matches = 0
            total_weight = 0
            
            for key, weight in cat_weights.items():
                if key in cat_i and key in cat_j:
                    total_weight += weight
                    if cat_i[key] == cat_j[key]:
                        weighted_matches += weight
            
            # 가중 평균 유사도 계산
            similarity = weighted_matches / total_weight if total_weight > 0 else 0
            
            cat_sim[i, j] = similarity
            cat_sim[j, i] = similarity  # 대칭 행렬 - j,i 위치에도 같은 값 설정
    
    # 3. 수치형 특성 유사도 계산 (거리 기반)
    num_sim = np.ones((n_samples, n_samples))
    
    for i in range(n_samples):
        for j in range(i+1, n_samples):
            num_i = features[i]['numerical']
            num_j = features[j]['numerical']
            
            # 나이 유사도 계산
            age_i = num_i['age_years']
            age_j = num_j['age_years']
            age_sim = 1.0  # 기본값은 완전 유사
            
            if age_i > 0 and age_j > 0:  # 둘 다 알 수 있는 경우만 계산
                age_diff = abs(age_i - age_j)
                # 나이 차이가 클수록 유사도 감소 (최대 5년 차이까지 고려)
                age_sim = max(0, 1 - (age_diff / 5.0))
            
            # 체중 유사도 계산
            weight_i = num_i['weight_kg']
            weight_j = num_j['weight_kg']
            weight_sim = 1.0  # 기본값은 완전 유사
            
            if weight_i > 0 and weight_j > 0:  # 둘 다 알 수 있는 경우만 계산
                weight_diff = abs(weight_i - weight_j)
                # 체중 차이가 클수록 유사도 감소 (최대 10kg 차이까지 고려)
                weight_sim = max(0, 1 - (weight_diff / 10.0))
            
            # 종합 수치형 유사도 (나이와 체중의 평균)
            similarity = (age_sim + weight_sim) / 2
            num_sim[i, j] = similarity
            num_sim[j, i] = similarity  # 대칭 행렬
    
    # 4. 가중치 적용하여 최종 유사도 행렬 계산
    text_weight = 0.6    # 텍스트 특성 (종류, 색상, 특징 등)이 가장 중요
    cat_weight = 0.3     # 범주형 특성 (성별, 중성화 여부 등)은 중간 중요도
    num_weight = 0.1     # 수치형 특성 (정확한 나이, 체중)은 보조적 역할
    
    sim_matrix = (text_weight * text_sim + 
                  cat_weight * cat_sim + 
                  num_weight * num_sim)
    
    # 5. 유사도 행렬 정규화 (0~1 범위로) since 일부 값이 1보다 커질 수 있으므로 최대값으로 나눔
    row_max = np.max(sim_matrix, axis=1, keepdims=True)
    sim_matrix = sim_matrix / np.where(row_max > 0, row_max, 1)
    
    return sim_matrix

async def _store_similar_animals_in_redis(animal_ids: list, sim_matrix, top_k: int):
    """
    각 동물에 대해 유사도가 높은 상위 top_k개 동물 ID를 Redis에 저장
    """
    for i, animal_id in enumerate(animal_ids):
        # i번째 동물의 모든 다른 동물과의 유사도 
        similarities = sim_matrix[i]

        # 유사도에 따라 내림차순 정렬하고 자기 자신(인덱스 i)은 제외
        sorted_indices = [idx for idx in np.argsort(similarities)[::-1] if idx != i]
        
        # 상위 top_k개만 선택
        top_indices = sorted_indices[:top_k]
        
        # 인덱스를 실제 동물 ID로 변환
        top_similar_ids = [animal_ids[idx] for idx in top_indices]

        key = f"similar:{animal_id}"
        await redis_client.delete(key)
        if top_similar_ids:
            await redis_client.rpush(key, *top_similar_ids)