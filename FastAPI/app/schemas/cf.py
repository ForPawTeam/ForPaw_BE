# app/schemas/cf.py
from pydantic import BaseModel

class InteractionDTO(BaseModel):
    user_id: int
    animal_id: int
    like_count: int
    view_count: int
    inquiry_count: int