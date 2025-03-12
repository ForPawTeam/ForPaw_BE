from fastapi import APIRouter
from app.api import animal_introduction
from app.api import animal_recommendation

api_router = APIRouter()
api_router.include_router(animal_introduction.router, prefix="/api")
api_router.include_router(animal_recommendation.router, prefix="/api")