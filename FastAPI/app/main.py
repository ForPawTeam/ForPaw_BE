# app/main.py
from fastapi import FastAPI
from app.tasks.scheduler import start_scheduler
from app.api import api_router
from app.services.cb import update_animal_similarity_data, init_redis as init_cb_redis
from app.services.cf import init_redis as init_cf_redis

async def lifespan(app: FastAPI):
    await init_cf_redis()
    await init_cb_redis()

    # 애플리케이션 시작 시 초기 콘텐츠 기반 추천 정보 갱신
    await update_animal_similarity_data(top_k=5)
    
    app.state.scheduler = start_scheduler()

    yield

    app.state.scheduler.shutdown()

app = FastAPI(lifespan=lifespan)
app.include_router(api_router)