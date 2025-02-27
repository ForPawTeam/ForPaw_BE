from fastapi import APIRouter
from app.api import animal
from app.api import cf

api_router = APIRouter()
api_router.include_router(animal.router, prefix="/api")
api_router.include_router(cf.router, prefix="/api")