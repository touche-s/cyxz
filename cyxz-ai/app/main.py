from fastapi import FastAPI

from app.api import review

app = FastAPI(title="cyxz-ai", version="0.1.0")

app.include_router(review.router)


@app.get("/health")
async def health():
    return {"status": "ok"}
