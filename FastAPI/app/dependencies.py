# app/dependencies.py
from fastapi import Request
from app.services.animal_introduction import AnimalIntroductionService

async def get_introduction_service(
    request: Request
) -> AnimalIntroductionService:
    # lifespan에서 초기화해 둔 싱글턴 인스턴스를 꺼내서 돌려줍니다.
    return request.app.state.intro_service