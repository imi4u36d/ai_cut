ARG PYTHON_IMAGE=docker.m.daocloud.io/library/python:3.12-slim-bookworm
FROM ${PYTHON_IMAGE}

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/

WORKDIR /app

COPY packages/backend_core /app/packages/backend_core
COPY apps/api /app/apps/api
COPY apps/worker /app/apps/worker

RUN pip install --upgrade pip \
    && pip install -e /app/packages/backend_core -e /app/apps/api -e /app/apps/worker

WORKDIR /app/apps/api

CMD ["uvicorn", "app.main:app", "--reload", "--host", "0.0.0.0", "--port", "8000"]
