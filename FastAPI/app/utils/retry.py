# app/utils/retry.py
import asyncio
import logging
from functools import wraps
from sqlalchemy.exc import SQLAlchemyError, OperationalError, DatabaseError
from pymysql.err import OperationalError as PyMySQLOperationalError

logger = logging.getLogger(__name__)

def with_db_retry(max_retries=3, retry_delay=1.0, backoff_factor=2.0):
    """
        max_retries (int): 최대 재시도 횟수
        retry_delay (float): 첫 번째 재시도 전 대기 시간(초)
        backoff_factor (float): 각 재시도마다 대기 시간에 곱해질 인자
    """
    def decorator(func):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            last_exception = None
            current_delay = retry_delay
            
            for attempt in range(max_retries + 1):  # 원래 시도 + 재시도
                try:
                    return await func(*args, **kwargs)
                except (SQLAlchemyError, PyMySQLOperationalError) as e:
                    last_exception = e
                    
                    # 연결 관련 오류인지 확인 (재시도할만한 오류)
                    is_connection_error = isinstance(e, OperationalError) or \
                                        isinstance(e, DatabaseError) or \
                                        "connection" in str(e).lower() or \
                                        "timeout" in str(e).lower() or \
                                        "reset by peer" in str(e).lower()
                    
                    # 마지막 시도이거나 재시도할 필요가 없는 오류인 경우
                    if attempt >= max_retries or not is_connection_error:
                        logger.error(f"Database operation failed after {attempt+1} attempts: {str(e)}")
                        raise last_exception
                    
                    # 다음 시도 전 로깅 및 대기
                    logger.warning(
                        f"Database operation failed (attempt {attempt+1}/{max_retries+1}): {str(e)}. "
                        f"Retrying in {current_delay:.2f}s..."
                    )
                    await asyncio.sleep(current_delay)
                    current_delay *= backoff_factor  # 대기 시간 증가
            
            # 이 코드는 실행되지 않아야 하지만, 안전을 위해 추가
            raise last_exception
        
        return wrapper
    return decorator