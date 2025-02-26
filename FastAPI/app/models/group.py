# app/models/group.py
from sqlalchemy import Column, Integer, String, Enum as PgEnum
from sqlalchemy.ext.declarative import declarative_base
from app.enums import Province, District

Base = declarative_base()

class Group(Base):
    __tablename__ = "groups_tb"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255))
    province = Column(PgEnum(Province), nullable=False)
    district = Column(PgEnum(District), nullable=False)
    sub_district = Column(String(255))
    description = Column(String(255))
    category = Column(String(255))
    profileURL = Column(String(255))
    participant_num = Column(Integer, default=0)
    like_num = Column(Integer, default=0)