import asyncio
import logging

logger = logging.getLogger(__name__)

class AsyncRateLimiter:
    def __init__(self, rate_limit: int, time_period: float = 60.0):
        self.rate_limit = rate_limit
        self.time_period = time_period
        self.tokens = rate_limit  # 현재 사용 가능한 토큰 수
        self.last_update = None   # 비동기 컨텍스트에서 초기화하기 위해 None으로 설정
        self.lock = asyncio.Lock() 
    
    async def acquire(self):
        """토큰을 획득하거나 사용 가능한 토큰이 생길 때까지 대기"""
        async with self.lock:
            now = asyncio.get_running_loop().time()  # 실행 중인 루프 가져오기

            # 첫 번째 호출 시 초기화
            if self.last_update is None:
                self.last_update = now
                self.tokens -= 1
                return

            elapsed = now - self.last_update
            
            # 경과 시간에 따라 토큰 보충
            self.tokens = min(
                self.rate_limit,
                self.tokens + int(elapsed * (self.rate_limit / self.time_period))
            )
            self.last_update = now
            
            # 토큰이 없으면 새 토큰이 생성될 때까지 대기
            if self.tokens <= 0:
                wait_time = self.time_period / self.rate_limit  
                await asyncio.sleep(wait_time)
                self.tokens = 1
            
            self.tokens -= 1  # 토큰 사용