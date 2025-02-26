# app/crud/animal.py
from sqlalchemy.future import select
from app.models.animal import Animal
from app.models.group import Group

async def find_animal_by_id(db, animal_id: int):
    result = await db.execute(select(Animal).filter(Animal.id == animal_id))
    return result.scalars().first()

async def find_all_animals(db):
    result = await db.execute(select(Animal).filter(Animal.removed_at.is_(None)))
    return result.scalars().all()

async def find_animal_ids_with_null_title(db):
    result = await db.execute(select(Animal.id).filter(Animal.introduction_title.is_(None)))
    return result.scalars().all()

async def find_group_by_id(db, group_id: int):
    result = await db.execute(select(Group).filter(Group.id == group_id))
    return result.scalars().first()

async def find_all_groups(db):
    result = await db.execute(select(Group))
    return result.scalars().all()
