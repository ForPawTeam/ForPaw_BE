# crud.py
from pydantic_settings import BaseSettings
import os

class Settings(BaseSettings):
    DATABASE_URL: str
    REDIS_HOST: str
    REDIS_PORT: int
    REDIS_DB: int
    OPENAI_API_KEY: str

    class Config:
        #env_file = os.path.join(os.path.dirname(__file__), "../.env")
        env_file = None

settings = Settings()