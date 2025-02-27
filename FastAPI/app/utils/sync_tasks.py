# app/utils/sync_tasks.py
import asyncio
from app.services.animal import update_animal_introductions

def sync_update_animal_introductions(animal_ids):
    asyncio.create_task(update_animal_introductions(animal_ids))