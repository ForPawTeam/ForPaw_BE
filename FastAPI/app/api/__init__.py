from fastapi import APIRouter
from app.api import animal

api_router = APIRouter()
api_router.include_router(animal.router, prefix="/api")