# app/db/redis.py
from redis.asyncio import Redis
from app.core.config import settings
import logging

logger = logging.getLogger(__name__)

# 단일 Redis 클라이언트 인스턴스
redis_client = None

async def init_redis():
    global redis_client
    
    if redis_client is not None:
        return redis_client
    
    redis_client = Redis(
        host=settings.REDIS_HOST,
        port=settings.REDIS_PORT,
        db=settings.REDIS_DB,
        decode_responses=True
    )
    
    return redis_client

async def get_redis_client():
    global redis_client
    if redis_client is None:
        return await init_redis()
    return redis_client

async def close_redis():
    global redis_client
    if redis_client is not None:
        await redis_client.close()
        redis_client = None