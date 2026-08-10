import asyncpg
from contextlib import asynccontextmanager
from fastapi import FastAPI

from app.config import settings


class DatabasePool:
    def __init__(self):
        self.pool: asyncpg.Pool | None = None

    async def connect(self):
        self.pool = await asyncpg.create_pool(settings.database_url)

    async def disconnect(self):
        if self.pool:
            await self.pool.close()


db = DatabasePool()


@asynccontextmanager
async def lifespan(app: FastAPI):
    await db.connect()
    yield
    await db.disconnect()
