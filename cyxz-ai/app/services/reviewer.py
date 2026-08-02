import base64
from urllib.parse import urlparse

import httpx

from app.config import (
    LLM_API_URL, LLM_API_KEY, LLM_MODEL, AUTO_PASS_WITHOUT_KEY,
    QWEN_VL_API_URL, QWEN_VL_API_KEY, QWEN_VL_MODEL,
)

# 图片下载安全限制
MAX_IMAGE_BYTES = 10 * 1024 * 1024  # 10MB，防大文件耗尽内存
BLOCKED_HOSTS = {"169.254.169.254", "metadata.google.internal", "metadata"}


def _validate_image_url(url: str) -> str | None:
    """校验图片 URL 安全性（防 SSRF），通过返回 None，否则返回错误信息"""
    try:
        parsed = urlparse(url)
    except Exception:
        return "图片地址格式错误"
    if parsed.scheme not in ("http", "https"):
        return "仅支持 http/https 图片地址"
    host = (parsed.hostname or "").lower()
    if not host or host in BLOCKED_HOSTS:
        return "图片地址不合法"
    return None

IMAGE_PROMPT = """你是二次元社区的AI图片审核员。判断此图是否违规。

违规项（任一命中即拒绝）：
- 色情低俗（裸露、性暗示）
- 暴力血腥（血腥场景、恐怖画面）
- 政治敏感
- 广告引流（二维码、微信号、水印广告）

请严格只回答：PASS 或 REJECT: 简短原因"""

PROMPT = """你是二次元社区的AI审核员。判断以下帖子内容是否违规。

违规项（任一命中即拒绝）：
- 色情低俗
- 暴力血腥
- 政治敏感
- 人身攻击、引战、辱骂
- 广告引流、垃圾信息

请严格只回答：PASS 或 REJECT: 简短原因

标题：{title}
正文：{content}"""


async def review_text(title: str, content: str) -> tuple[bool, str]:
    """调用 LLM 审核文本，返回 (passed, reason)"""
    if not LLM_API_KEY:
        return (True, "") if AUTO_PASS_WITHOUT_KEY else (False, "审核服务未配置")

    prompt = PROMPT.format(title=title, content=content[:2000])

    async with httpx.AsyncClient(timeout=30.0) as client:
        resp = await client.post(
            LLM_API_URL,
            headers={
                "Authorization": f"Bearer {LLM_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": LLM_MODEL,
                "messages": [
                    {"role": "system", "content": "你是严格的审核员，只回答 PASS 或 REJECT: 原因。"},
                    {"role": "user", "content": prompt},
                ],
                "temperature": 0.0,
                "max_tokens": 50,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        answer = data["choices"][0]["message"]["content"].strip()

    if answer.upper().startswith("PASS"):
        return True, ""
    else:
        reason = answer.split(":", 1)[-1].strip() if ":" in answer else answer
        return False, reason


async def review_image(image_url: str) -> tuple[bool, str]:
    """调用通义千问 VL 审核图片，返回 (passed, reason)
    
    先将图片下载为 base64 再传给千问，避免 OSS 暴露公网。
    """
    if not QWEN_VL_API_KEY:
        return (True, "") if AUTO_PASS_WITHOUT_KEY else (False, "图像审核服务未配置")

    # SSRF 防护：校验图片地址
    err = _validate_image_url(image_url)
    if err:
        return False, err

    # 下载图片并转 base64，限制大小防耗尽内存
    async with httpx.AsyncClient(timeout=15.0, follow_redirects=True) as client:
        img_resp = await client.get(image_url)
        img_resp.raise_for_status()
        img_bytes = img_resp.content
        if len(img_bytes) > MAX_IMAGE_BYTES:
            return False, "图片过大"

    mime = "image/jpeg"
    if img_bytes[:4] == b"\x89PNG":
        mime = "image/png"
    elif img_bytes[:4] == b"RIFF":
        mime = "image/webp"
    elif img_bytes[:3] == b"GIF":
        mime = "image/gif"

    data_uri = f"data:{mime};base64,{base64.b64encode(img_bytes).decode()}"

    async with httpx.AsyncClient(timeout=30.0) as client:
        resp = await client.post(
            QWEN_VL_API_URL,
            headers={
                "Authorization": f"Bearer {QWEN_VL_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "model": QWEN_VL_MODEL,
                "messages": [
                    {"role": "system", "content": "你是严格的审核员，只回答 PASS 或 REJECT: 原因。"},
                    {
                        "role": "user",
                        "content": [
                            {"type": "image_url", "image_url": {"url": data_uri}},
                            {"type": "text", "text": IMAGE_PROMPT},
                        ],
                    },
                ],
                "temperature": 0.0,
                "max_tokens": 50,
            },
        )
        resp.raise_for_status()
        data = resp.json()
        answer = data["choices"][0]["message"]["content"].strip()

    if answer.upper().startswith("PASS"):
        return True, ""
    else:
        reason = answer.split(":", 1)[-1].strip() if ":" in answer else answer
        return False, reason
