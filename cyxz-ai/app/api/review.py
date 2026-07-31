from fastapi import APIRouter
from pydantic import BaseModel

from app.services.reviewer import review_text, review_image

router = APIRouter(prefix="/review", tags=["review"])


class ReviewRequest(BaseModel):
    post_id: int
    title: str
    content: str


class ReviewResponse(BaseModel):
    passed: bool
    reason: str = ""


class ImageReviewRequest(BaseModel):
    post_id: int
    image_url: str


@router.post("", response_model=ReviewResponse)
async def review(request: ReviewRequest):
    """AI 审核帖子文本"""
    passed, reason = await review_text(request.title, request.content)
    return ReviewResponse(passed=passed, reason=reason)


@router.post("/image", response_model=ReviewResponse)
async def review_image_endpoint(request: ImageReviewRequest):
    """AI 审核图片"""
    passed, reason = await review_image(request.image_url)
    return ReviewResponse(passed=passed, reason=reason)
