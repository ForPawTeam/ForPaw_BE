# tasks/scheduler.py
from apscheduler.schedulers.asyncio import AsyncIOScheduler
import asyncio
from app.services.animal import update_animal_similarity_data

def start_scheduler():
    scheduler = AsyncIOScheduler()
    scheduler.add_job(
        lambda: asyncio.create_task(update_animal_similarity_data(top_k=5)),
        'cron',
        hour=1
    )
    scheduler.start()
    return scheduler