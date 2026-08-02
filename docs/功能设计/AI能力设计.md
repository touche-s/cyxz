# AI 能力设计

> AI 能力是次元小站的内容安全中枢。本文档分已实现和规划两部分，已实现内容均基于代码核对。

## 设计定位

AI 能力的核心目标是 **内容安全审核**，而非花哨的生成能力。原因：

1. **合规优先**：社区产品上线前必须有内容审核能力，违规内容自动拦截
2. **降本增效**：AI 初筛替代人工初审，管理员只处理边界 case
3. **架构解耦**：AI 服务独立部署，Java 主业务不依赖 AI 可用性
4. **fail-closed 原则**：AI 不可用时拒绝发布，绝不让违规内容自动放行

---

## 一、已实现

> 基于 `cyxz-ai`（Python FastAPI）+ `cyxz-post/AiReviewService` 代码核对。

### 1. 整体架构

```
用户发帖
  ↓
cyxz-post PostServiceImpl.createPost
  ├─ 入库 status=PENDING
  └─ CompletableFuture.runAsync 异步调 AI 审核
       ↓
       AiReviewService.review() (Java, RestTemplate)
       ├─ 文本审核: POST http://cyxz-ai:8000/review
       └─ 图片审核: POST http://cyxz-ai:8000/review/image (逐张)
            ↓
            cyxz-ai (Python FastAPI)
            ├─ review_text: DeepSeek LLM 文本审核
            └─ review_image: 通义千问 VL 多模态图片审核
       ↓
       handleReviewResult / handleReviewFailure
       ├─ 通过: status=APPROVED, 发 ES 同步, 发 POST_APPROVED 通知
       ├─ 拒绝: status=REJECTED, 记 reason, 发 POST_REJECTED 通知
       └─ 异常: 保持 PENDING, 标记"待人工审核"
```

关键设计：

- **异步审核**：`CompletableFuture.runAsync` 不阻塞发帖响应
- **fail-closed**：AI 返回空或序列化失败时 `AiReviewResult(false, "审核服务异常")`
- **人工兜底**：AI 异常时保持 `PENDING`，管理员在后台 `/post/admin/review/pending` 手动处理
- **纯函数服务**：`AiReviewService` 只调 Python AI，不碰 DB/缓存/MQ，职责单一

### 2. cyxz-ai Python 服务

**技术栈**（`requirements.txt`）：

- `fastapi==0.115.0` Web 框架
- `uvicorn==0.30.0` ASGI 服务器
- `httpx==0.27.0` 异步 HTTP 客户端
- `pydantic==2.9.0` 数据校验
- `python-dotenv==1.0.1` 环境变量加载

**配置**（`app/config.py`）：

```python
LLM_API_URL = "https://api.deepseek.com/v1/chat/completions"
LLM_MODEL = "deepseek-chat"
QWEN_VL_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
QWEN_VL_MODEL = "qwen3.7-plus"
AUTO_PASS_WITHOUT_KEY = false  # 未配置 key 时默认拒绝
```

**接口**（`app/api/review.py`）：

| 接口 | 请求 | 响应 |
|---|---|---|
| `POST /review` | `{post_id, title, content}` | `{passed, reason}` |
| `POST /review/image` | `{post_id, image_url}` | `{passed, reason}` |
| `GET /health` | — | `{status: "ok"}` |

### 3. 文本审核（review_text）

调用 DeepSeek LLM，prompt 固定为审核员角色：

```
你是二次元社区的AI审核员。判断以下帖子内容是否违规。
违规项（任一命中即拒绝）：
- 色情低俗 / 暴力血腥 / 政治敏感
- 人身攻击、引战、辱骂
- 广告引流、垃圾信息
请严格只回答：PASS 或 REJECT: 简短原因
```

- `temperature=0.0` 保证结果稳定
- `max_tokens=50` 限制输出长度，节省成本
- 内容超 2000 字截断
- 解析 `PASS` / `REJECT: 原因` 两种格式

### 4. 图片审核（review_image）

调用通义千问 VL 多模态模型：

- **SSRF 防护**：`_validate_image_url` 校验 scheme 和 host，屏蔽 `169.254.169.254` 等元数据服务地址
- **大小限制**：`MAX_IMAGE_BYTES = 10MB` 防大文件耗尽内存
- **下载转 base64**：避免 OSS 暴露公网，内网下载后转 data URI 传给千问
- **MIME 自动识别**：通过文件头识别 jpeg/png/webp/gif
- **逐张审核**：Java 侧循环调用，任一不通过即整体拒绝

### 5. Java 侧调用（AiReviewService）

```java
public AiReviewResult review(Long postId, String title, String content, List<String> imageUrls) {
    AiReviewResult textResult = callTextReview(postId, title, content);
    if (!textResult.passed) return textResult;
    
    if (imageUrls != null && !imageUrls.isEmpty()) {
        for (String imageUrl : imageUrls) {
            AiReviewResult imgResult = callImageReview(postId, imageUrl);
            if (!imgResult.passed) return imgResult;
        }
    }
    return new AiReviewResult(true, "");
}
```

- **文本先审**：文本不通过直接返回，不浪费图片审核调用
- **图片逐张**：任一不通过即整体拒绝
- **配置外置**：`ai.review.url` / `ai.review.image-url` 可通过 `application.yml` 覆盖

### 6. 审核结果处理

`PostServiceImpl` 的 `handleReviewResult` / `handleReviewFailure`：

| 场景 | 处理 |
|---|---|
| AI 通过 | `status=APPROVED`，清 `reviewReason`，清缓存，发 ES 同步（CREATE），发 `POST_APPROVED` 通知 |
| AI 拒绝 | `status=REJECTED`，记 `reviewReason`，清缓存，发 `POST_REJECTED` 通知 |
| AI 异常 | 保持 `PENDING`，标记 `reviewReason="AI审核服务异常，待人工审核"` |
| 状态已变更 | 跳过处理（防重复） |

通知通过 `sendReviewNotify` 走 RabbitMQ 异步发送，`senderId=0L` 表示系统通知。

---

## 二、规划

### 1. 创作辅助能力

| 能力 | 触发方式 | 输入 | 输出 |
|---|---|---|---|
| 标题润色 | 发布页按钮 | 标题 + 正文 | 3 个优化标题 |
| 标签建议 | 上传图片后 / 按钮 | 图片 + 正文 | 5 个建议标签 |
| 圈子建议 | 填写正文后 | 标题 + 正文 + 图片 | 1-3 个推荐圈子 |
| 正文润色 | 长文模式按钮 | 正文 | 优化表达、修正错别字 |
| AI 摘要 | 长文发布时 | 完整正文 | 一段摘要显示在卡片上 |

### 2. 圈子 RAG 知识库

**知识分层**：

| 层级 | 内容 | 更新频率 | 检索方式 |
|---|---|---|---|
| IP 百科层 | 角色设定、世界观、机制、剧情 | 爬虫灌入，不常变 | RAG 语义检索 |
| 社区精选层 | 圈规、公告、精华帖、高赞攻略 | 管理员 + 自动筛选 | RAG 语义检索 |
| 实时搜索层 | 最新讨论帖 | 实时 | ES 关键词/语义 |

**入库流程**：爬虫脚本 → 清洗 → 分块（chunk_size=512, overlap=64）→ Embedding → 存 ES `dense_vector` 字段 + `circle_id` 过滤

**检索流程**：embedding 用户问题 → ES 向量检索（top_k=5）→ rerank（可选）→ 拼接 prompt → LLM 生成回答（附来源引用）

### 3. 次元小助手 Agent

| 能力 | 工具 | 说明 |
|---|---|---|
| 找圈子 | `search_circles(keyword)` | 搜圈子，返回名称+简介 |
| 查帖子 | `search_posts(query, circleId?)` | ES 关键词 + 语义搜索 |
| 查热帖 | `get_hot_posts(circleId?)` | 热门帖子列表 |
| 建议标签 | `suggest_tags(content)` | AI 生成 5 个标签 |
| 润色标题 | `polish_title(title, content)` | 优化标题吸引力 |
| 查圈规 | 走该圈 RAG | 回答圈规相关问题 |
| 查设定 | 走该圈 IP 百科 RAG | 回答角色/剧情/机制问题 |

**上下文感知**：根据用户当前位置（圈子详情页/首页/发布页）自动切换 Agent 行为。

**记忆系统**：Redis 存最近 20 轮对话（TTL 30min），MySQL 存用户偏好（永久）。

### 4. 运营辅助

- **圈报生成**：每天/每周自动生成圈子摘要（新增帖子、最热帖子、新成员、热门标签）
- **话题推荐**：基于近期热帖 + 圈子特性，AI 推荐下周话题活动

### 5. 模型选型

**推荐方案：阿里云全家桶**

| 组件 | 阿里云产品 | 用途 |
|---|---|---|
| 文本对话/Agent | 通义千问 qwen-plus | 对话、标签建议、标题润色 |
| 图片理解 | 通义千问 qwen-vl-plus | 多模态，COS 角色识别 |
| 文本 Embedding | text-embedding-v2 | RAG 知识库向量化 |
| 图片/文本审核 | 内容安全（独立产品） | 色情/暴力/政治/广告检测 |
| 文件存储 | OSS | 替代 MinIO，内网互通 |

当前已用 DeepSeek（文本）+ 通义千问 VL（图片），未来可平滑切换到阿里云全家桶。

---

## 三、技术亮点（简历视角）

1. **跨语言架构**：Java 主业务 + Python AI 服务，HTTP 解耦，各自用最擅长的语言
2. **fail-closed 安全策略**：AI 不可用时拒绝发布，绝不让违规内容自动放行
3. **异步审核**：`CompletableFuture.runAsync` 不阻塞发帖响应，用户体验好
4. **人工兜底**：AI 异常时保持 `PENDING` 转人工，避免误判放行
5. **SSRF 防护**：图片审核校验 URL scheme 和 host，屏蔽元数据服务地址
6. **多模态审核**：文本走 LLM，图片走 VL 模型，逐张审核任一不通过即拒绝
7. **prompt 工程**：`temperature=0.0` + `max_tokens=50` 保证结果稳定且低成本
8. **纯函数服务**：`AiReviewService` 只调 AI，不碰 DB/缓存/MQ，职责单一易测试

---

## 四、实施路线

```
当前（已实现）
  ├─ Python cyxz-ai 服务（FastAPI）✓
  ├─ 文本审核（DeepSeek LLM）✓
  ├─ 图片审核（通义千问 VL）✓
  ├─ Java 异步调用 + fail-closed ✓
  ├─ 人工兜底（PENDING 转后台）✓
  └─ 审核结果通知（MQ 异步）✓

第二步（创作辅助）
  ├─ 标题润色
  ├─ 标签建议
  ├─ 圈子建议
  └─ 长文摘要

第三步（圈子 RAG）
  ├─ 爬虫灌入 IP 百科
  ├─ ES dense_vector 索引
  ├─ 次元小助手 Agent
  └─ 上下文感知 + 对话记忆

第四步（运营辅助）
  ├─ 圈报生成
  ├─ 话题推荐
  └─ 内容聚类
```
