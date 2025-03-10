# app/crud/animal.py
import datetime
import logging
from sqlalchemy.future import select
from app.models.animal import Animal
from app.models.group import Group
from sqlalchemy.exc import SQLAlchemyError
from app.utils.retry import with_db_retry

logger = logging.getLogger(__name__)

@with_db_retry(max_retries=3)
async def find_animal_by_id(db, animal_id: int):
    result = await db.execute(
        select(Animal).filter(Animal.id == animal_id)
    )
    return result.scalars().first()

@with_db_retry(max_retries=3)
async def find_all_animals(db):
    result = await db.execute(
        select(Animal).filter(Animal.removed_at.is_(None))
    )
    return result.scalars().all()

@with_db_retry(max_retries=3)
async def find_recent_animal_ids_with_null_title(db):
    try:
        yesterday = datetime.datetime.now() - datetime.timedelta(days=1)
        result = await db.execute(
            select(Animal.id).filter(
                Animal.introduction_title.is_(None),
                Animal.created_date >= yesterday
            )
        )
        return result.scalars().all()
    except SQLAlchemyError as e:
        logger.error(f"Error finding animals without introduction: {str(e)}")
        raise

@with_db_retry(max_retries=3)
async def find_group_by_id(db, group_id: int):
    result = await db.execute(
        select(Group).filter(Group.id == group_id)
    )
    return result.scalars().first()

@with_db_retry(max_retries=3)
async def find_all_groups(db):
    result = await db.execute(
        select(Group)
    )
    return result.scalars().all()

@with_db_retry(max_retries=3)
async def update_animal_introduction(db, animal_id: int, title: str, content: str):
    try:
        animal = await find_animal_by_id(db, animal_id)
        if animal:
            animal.introduction_title = title
            animal.introduction_content = content
            db.add(animal)
            await db.commit()
            return True
        return False
    except SQLAlchemyError as e:
        await db.rollback()
        logger.error(f"Error updating animal introduction for ID {animal_id}: {str(e)}")
        raise