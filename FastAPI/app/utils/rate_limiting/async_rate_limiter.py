import asyncio

"""분당 호출 가능 횟수를 제한 => 배치당 동시성 제한을 줬지만 API LIMIT를 2차적으로 방어하기 위해 사용"""
class RateLimiter:
    def __init__(self, max_calls_per_period: int, period_seconds: float = 60.0):
        self.max_calls = max_calls_per_period
        self.period = period_seconds
        self.available_calls = max_calls_per_period  # 현재 사용 가능한 토큰 수
        self.last_refill = None   # 비동기 컨텍스트에서 초기화하기 위해 None으로 설정
        self._lock = asyncio.Lock() 
    
    async def acquire(self):
        async with self._lock:
            now = asyncio.get_running_loop().time()  # 실행 중인 루프 가져오기

            # 첫 번째 호출 시 last_refill 초기화
            if self.last_refill is None:
                self.last_refill = now

            # 경과 시간에 따라 토큰 보충
            elapsed = now - self.last_refill
            refill_tokens = int(elapsed * (self.max_calls / self.period))
            if refill_tokens > 0:
                self.available_calls = min(self.max_calls, self.available_calls + refill_tokens)
                self.last_refill = now
            
            # 토큰이 없으면 새 토큰이 생성될 때까지 대기
            if self.available_calls <= 0:
                wait_time = self.period / self.max_calls  
                await asyncio.sleep(wait_time)
                self.available_calls = 1
                self.last_refill = asyncio.get_running_loop().time()
            
            # 토큰 사용
            self.available_calls -= 1  