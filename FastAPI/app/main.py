# app/main.py
from fastapi import FastAPI
from apscheduler.schedulers.asyncio import AsyncIOScheduler
import asyncio
from app.api import api_router
from app.services.animal import update_animal_similarity_data

app = FastAPI()

async def lifespan(app: FastAPI):
    await update_animal_similarity_data(top_k=5)
    
    scheduler = AsyncIOScheduler()
    scheduler.add_job(
        lambda: asyncio.create_task(update_animal_similarity_data(top_k=5)),
        'cron',
        hour=1
    )
    scheduler.start()
    app.state.scheduler = scheduler

    yield

    scheduler.shutdown()

app.include_router(api_router)