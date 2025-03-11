from fastapi import APIRouter
from app.api import animal
from app.api import recommendation

api_router = APIRouter()
api_router.include_router(animal.router, prefix="/api")
api_router.include_router(recommendation.router, prefix="/api")