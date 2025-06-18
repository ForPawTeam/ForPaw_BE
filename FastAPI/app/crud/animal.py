# app/crud/animal.py
import datetime
import logging
from sqlalchemy.future import select
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession
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
async def find_animals_by_ids(db, ids: list[int]):
    result = await db.execute(
        select(Animal).filter(Animal.id.in_(ids))
    )
    return result.scalars().all()

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
    animal = await find_animal_by_id(db, animal_id)
    if animal:
        animal.introduction_title = title
        animal.introduction_content = content
        db.add(animal)
        return True
    return False

async def bulk_update_animal_introductions(db: AsyncSession, records: list[dict]):
    if not records:
        return 0

    sql = text("""
        UPDATE animal_tb
            SET introduction_title   = :title,
                introduction_content = :content
        WHERE id = :id
    """)

    params = [
        {"id": rec["b_id"],
        "title": rec["title_param"],
        "content": rec["content_param"]}
        for rec in records
    ]
    await db.execute(sql, params)
    await db.commit()
    return len(params)