# app/api/animal_introduction.py
from fastapi import APIRouter, BackgroundTasks
from app.services.animal_introduction import AnimalIntroductionService, get_introduction_service, update_animal_introductions, find_animals_without_introduction
from fastapi import Depends

router = APIRouter()

@router.post("/introduce/animal")
async def process_animal_introduction(
    background_tasks: BackgroundTasks,
    service: AnimalIntroductionService = Depends(get_introduction_service)
): 
    animal_ids = await find_animals_without_introduction()
    background_tasks.add_task(update_animal_introductions, animal_ids, service)
    
    return {"success": "true", "code": 200, "message": "OK", "result": "null"}