import logging
from datetime import datetime

import asyncpg
import httpx
from fastapi import BackgroundTasks, FastAPI, File, Form, HTTPException, UploadFile
from pydantic import BaseModel, EmailStr, Field

from app import wigle_client
from app.crypto import decrypt_token, encrypt_token
from app.database import db, lifespan

logger = logging.getLogger("warwalking")

app = FastAPI(title="WarWalker Tracker Backend", lifespan=lifespan)


# --- Pydantic models ---

class UserCreate(BaseModel):
    username: str = Field(..., max_length=50)
    email: EmailStr
    wigle_api_name: str
    wigle_api_token: str


class EventCreate(BaseModel):
    title: str = Field(..., max_length=100, json_schema_extra={"example": "Weekend Wireless Warrior"})
    description: str | None = Field(None, json_schema_extra={"example": "Walk and log networks this weekend."})
    start_time: datetime
    end_time: datetime


# --- Background work ---

async def upload_to_wigle_background(
    session_id: int,
    api_name: str,
    encrypted_token: str,
    file_bytes: bytes,
    file_name: str,
):
    try:
        api_token = decrypt_token(encrypted_token)
        result = await wigle_client.upload_session_file(api_name, api_token, file_bytes, file_name)
        wigle_file_id = result.get("transid", "UPLOADED")

        async with db.pool.acquire() as conn:
            await conn.execute(
                "UPDATE walk_sessions SET wigle_file_id = $1 WHERE session_id = $2",
                wigle_file_id, session_id,
            )
        logger.info("WiGLE upload complete for session %s (transid=%s)", session_id, wigle_file_id)
    except Exception:
        logger.exception("WiGLE upload failed for session %s", session_id)


# --- User endpoints ---

@app.post("/api/users/register", status_code=201)
async def register_user(user: UserCreate):
    """Registers a researcher after live-checking their WiGLE credentials."""
    try:
        await wigle_client.verify_credentials(user.wigle_api_name, user.wigle_api_token)
    except wigle_client.WigleAuthError as exc:
        raise HTTPException(status_code=401, detail=str(exc))
    except httpx.RequestError:
        raise HTTPException(status_code=502, detail="Unable to reach WiGLE authentication servers.")

    encrypted_token = encrypt_token(user.wigle_api_token)

    async with db.pool.acquire() as conn:
        try:
            user_id = await conn.fetchval(
                """INSERT INTO users (username, email, wigle_api_name, wigle_api_token_encrypted)
                   VALUES ($1, $2, $3, $4) RETURNING user_id;""",
                user.username, user.email, user.wigle_api_name, encrypted_token,
            )
        except asyncpg.UniqueViolationError:
            raise HTTPException(status_code=400, detail="Username or email already registered.")

    return {"status": "success", "user_id": user_id, "username": user.username}


@app.post("/api/sessions/sync")
async def sync_walk_session(
    background_tasks: BackgroundTasks,
    user_id: int = Form(...),
    start_time: str = Form(...),
    end_time: str = Form(...),
    steps_counted: int = Form(..., ge=0),
    ap_discovered: int = Form(..., ge=0),
    file: UploadFile = File(...),
):
    """Accepts a completed walk session, scores it locally, then syncs the log to WiGLE in the background."""
    dt_start = datetime.fromisoformat(start_time)
    dt_end = datetime.fromisoformat(end_time)
    if dt_end <= dt_start:
        raise HTTPException(status_code=400, detail="end_time must be after start_time.")

    async with db.pool.acquire() as conn:
        user_row = await conn.fetchrow(
            "SELECT wigle_api_name, wigle_api_token_encrypted FROM users WHERE user_id = $1", user_id
        )
        if not user_row:
            raise HTTPException(status_code=404, detail="Researcher account not found.")

        session_id = await conn.fetchval(
            """INSERT INTO walk_sessions (user_id, start_time, end_time, steps_counted, ap_discovered)
               VALUES ($1, $2, $3, $4, $5) RETURNING session_id;""",
            user_id, dt_start, dt_end, steps_counted, ap_discovered,
        )

    file_bytes = await file.read()

    background_tasks.add_task(
        upload_to_wigle_background,
        session_id,
        user_row["wigle_api_name"],
        user_row["wigle_api_token_encrypted"],
        file_bytes,
        file.filename,
    )

    return {
        "status": "accepted",
        "session_id": session_id,
        "message": "Session scored. WiGLE sync running in background.",
    }


@app.get("/api/leaderboard")
async def get_leaderboard():
    async with db.pool.acquire() as conn:
        rows = await conn.fetch("SELECT * FROM leaderboard LIMIT 50;")
        return [dict(row) for row in rows]


# --- Event endpoints ---

@app.post("/api/events", status_code=201)
async def create_event(event: EventCreate):
    if event.start_time >= event.end_time:
        raise HTTPException(status_code=400, detail="start_time must be before end_time.")

    async with db.pool.acquire() as conn:
        event_id = await conn.fetchval(
            """INSERT INTO event_windows (title, description, start_time, end_time)
               VALUES ($1, $2, $3, $4) RETURNING event_id;""",
            event.title, event.description, event.start_time, event.end_time,
        )
        return {"status": "created", "event_id": event_id}


@app.get("/api/events/active")
async def get_active_events():
    now = datetime.now()
    async with db.pool.acquire() as conn:
        rows = await conn.fetch(
            """SELECT event_id, title, description, start_time, end_time
               FROM event_windows
               WHERE end_time > $1
               ORDER BY start_time ASC;""",
            now,
        )
        return [dict(row) for row in rows]


@app.get("/api/events/{event_id}/leaderboard")
async def get_specific_event_leaderboard(event_id: int):
    async with db.pool.acquire() as conn:
        exists = await conn.fetchval("SELECT EXISTS(SELECT 1 FROM event_windows WHERE event_id = $1);", event_id)
        if not exists:
            raise HTTPException(status_code=404, detail="Event not found.")

        rows = await conn.fetch("SELECT * FROM get_event_leaderboard($1);", event_id)
        return [dict(row) for row in rows]
