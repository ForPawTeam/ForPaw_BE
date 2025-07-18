# app/main.py
from fastapi import FastAPI, Request
from app.api import api_router
from app.services.cb import update_animal_similarity_data
from app.db.redis import init_redis, close_redis
from app.services.animal_introduction import AnimalIntroductionService

async def lifespan(app: FastAPI):
    await init_redis()

    app.state.intro_service = AnimalIntroductionService()

    # 애플리케이션 시작 시 초기 콘텐츠 기반 추천 정보 갱신
    await update_animal_similarity_data(top_k=5)
    yield

    await close_redis()

app = FastAPI(lifespan=lifespan)
app.include_router(api_router)

async def get_introduction_service(request: Request) -> AnimalIntroductionService:
    return request.app.state.intro_service