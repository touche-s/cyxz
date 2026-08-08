# ============================================================
# Cyxz AI 审核服务 Dockerfile（Python / FastAPI）
# ============================================================
FROM python:3.11-slim
WORKDIR /app

COPY cyxz-ai/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY cyxz-ai/ .

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
