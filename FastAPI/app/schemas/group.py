# app/schemas/group.py
from pydantic import BaseModel

class GroupResponse(BaseModel):
    id: int
    name: str
    province: str
    district: str