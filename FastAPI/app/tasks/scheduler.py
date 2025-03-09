# tasks/scheduler.py
import asyncio
import logging
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.interval import IntervalTrigger
from app.services.cb import update_animal_similarity_data

logger = logging.getLogger(__name__)

async def scheduled_update():
    max_retries = 3
    for attempt in range(max_retries):
        try:
            await update_animal_similarity_data(top_k=5)
            logger.info("콘텐츠 기반 추천 정보 갱신 작업 완료")
            break
        except Exception as e:
            if attempt < max_retries - 1:
                await asyncio.sleep(5)
            else:
                logger.error("최대 재시도 횟수 초과: 콘텐츠 기반 추천 정보 갱신 작업 실패")

def start_scheduler():
    scheduler = AsyncIOScheduler(timezone="Asia/Seoul")
    trigger = IntervalTrigger(hours=1)
    scheduler.add_job(
        lambda: asyncio.create_task(scheduled_update()),
        trigger=trigger,
        id="update_cb_data"
    )
    try:
        scheduler.start()
        logger.info("스케줄러 시작 성공")
    except Exception as e:
        logger.exception("스케줄러 시작 중 오류 발생: %s", e)
        raise e
    return scheduler
