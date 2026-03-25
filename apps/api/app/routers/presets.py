from __future__ import annotations

from fastapi import APIRouter, Request

from backend_core.schemas import TaskPreset


router = APIRouter(tags=["presets"])


@router.get("/presets", response_model=list[TaskPreset])
def list_presets(request: Request) -> list[TaskPreset]:
    return request.app.state.runtime.service.list_task_presets()
