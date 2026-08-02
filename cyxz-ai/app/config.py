import os
from dotenv import load_dotenv

# 加载项目根目录 cyxz/.env（已 .gitignore）
load_dotenv(os.path.join(os.path.dirname(__file__), "..", "..", ".env"))

# LLM 审核配置
LLM_API_KEY = os.getenv("LLM_API_KEY", "")
LLM_API_URL = os.getenv("LLM_API_URL", "https://api.deepseek.com/v1/chat/completions")
LLM_MODEL = os.getenv("LLM_MODEL", "deepseek-chat")

# 审核策略：未配置 API key 时默认拒绝（fail-closed，避免违规内容自动放行）
AUTO_PASS_WITHOUT_KEY = os.getenv("AUTO_PASS_WITHOUT_KEY", "false").lower() in ("true", "1", "yes")

# 图像审核配置（通义千问 VL，兼容 OpenAI 格式）
QWEN_VL_API_KEY = os.getenv("QWEN_VL_API_KEY", "")
QWEN_VL_API_URL = os.getenv("QWEN_VL_API_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
QWEN_VL_MODEL = os.getenv("QWEN_VL_MODEL", "qwen3.7-plus")
