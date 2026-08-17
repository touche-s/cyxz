# 提交规范（COMMIT_CONVENTION）

> 本规范依据本项目 Git 历史实际风格整理，采用 **Conventional Commits** 格式。
> 核心要求：**每次提交必须包含 body**，用于说明「为什么改」与「影响面」，便于日后回溯与对外展示。

---

## 一、格式总览

```
<type>(<scope>): <subject>

<body>
```

示例：

```
fix(post): ES 同步失败入队使用正确 action 事件

失败消息在 try 块外按真实 action 构造再入队，
避免"下架帖补偿重试变重新创建"的错乱；补单测覆盖 DELETE 分支。
```

---

## 二、type（必填）

| type | 用途 | 示例 |
|---|---|---|
| `feat` | 新功能 / 新页面 / 新接口 | `feat(frontend): Token 过期静默续期，并发请求单飞刷新` |
| `fix` | 缺陷修复 | `fix(comment): 评论计数增量改事务提交后执行，避免回滚脏数据` |
| `perf` | 性能优化 | `perf(gateway): 限流阈值 200/400 上调至 500/600` |
| `refactor` | 重构，行为不变 | `refactor: 事务后副作用统一 TransactionUtils.afterCommit` |
| `docs` | 文档（md / 注释） | `docs: 新增缓存优化验证报告与限流验证报告` |
| `test` | 测试相关 | `test(benchmark): 新增 k6 压测脚本与结果分析工具` |
| `ci` | CI 配置 / 构建脚本 | `ci: 全模块单元测试纳入 CI` |
| `build` | 构建依赖 / Docker 镜像 | `build(docker): maven-deps 复用已有镜像依赖并离线编译` |
| `chore` | 杂项（忽略规则、产物清理等） | `chore(benchmark): 忽略压测 CSV 产物` |
| `revert` | 回滚提交 | `revert: 移除全链路 traceId，保留幂等保护和 logback 合并` |

> 一个提交只表达一个意图；同时含多种类型时，以主意图为准。

---

## 三、scope（可选，建议填写）

按**模块 / 技术面**限定影响范围：

| scope | 模块 |
|---|---|
| `frontend` | cyxz-frontend（Vue3 前端） |
| `common` / `security` | cyxz-common / cyxz-security 共享模块 |
| `gateway` | cyxz-gateway 网关 |
| `auth` / `user` / `post` / `comment` / `circle` / `message` / `search` / `upload` | 各业务微服务 |
| `governance` / `audit` / `analytics` | B 端治理三服务 |
| `db` | db/init.sql 等数据库脚本 |
| `docker` | docker-compose.yml、docker/ 部署文件 |
| `docs` | docs/ 下文档 |
| `config` | 配置（application.yml、.env.example 等） |
| `benchmark` | 压测脚本与报告 |

跨模块全局改动可省略 scope（如 `fix:`、`docs:`）。

---

## 四、subject 规则（必填）

1. **中文**，简洁、动宾结构，一句话说清「做了什么 + 为什么」
2. 控制在 **50 字以内**，末尾不加句号
3. 面向阅读者表述，避免堆砌技术黑话（例：`fix: 关系表 id 改为自增，修复点赞/关注/入圈首次写入失败`）

---

## 五、body 规则（必填）

body 与 subject 之间空一行，逐条说明：

| 要点 | 说明 |
|---|---|
| **为什么** | 背景 / 动机：修了什么 bug、为什么这样设计 |
| **怎么改** | 关键做法，必要时标注涉及文件或函数 |
| **影响面** | 涉及模块、接口、数据、是否需迁移；**破坏性变更必须标注** |
| **验证** | 单测、压测、部署验证结果（有则写） |

格式要求：

- 单行建议 ≤ 72 字符（中文按自然句换行）
- 可多段；需要强调的部分用 `-` 列表
- **BREAKING CHANGE**：存在破坏性变更时，在 body 末尾单独一段注明 `BREAKING CHANGE: <说明>`（如接口路径变更、SQL 迁移、依赖升级）

---

## 六、提交粒度与拆分

1. **主题化小提交**：按功能 / 问题独立成条，避免大而全的「refactor all」
2. **代码 + 对应测试 + 文档同步**：同一改动涉及单测或文档时，放入同一个提交，保持提交自洽
3. **避免混入无关改动**：格式化、依赖升级、文档修稿等与主题无关的变更单独成条
4. 为 GitHub 展示效果，提交信息聚焦且可读，历史保持干净

---

## 七、命令速查

```bash
# 多行 body 提交（推荐）
git commit -m "<type>(<scope>): <subject>" -m "<body>"

# 带 BREAKING CHANGE 标注
git commit -m "<type>(<scope>): <subject>" -m "<body>" -m "BREAKING CHANGE: <说明>"

# 交互式编写（git commit 后自动进入编辑器，写 subject + body）
git commit
```

---

## 八、提交前自检清单

- [ ] type 正确且只有一个
- [ ] scope 填写模块名（跨模块可省略）
- [ ] subject ≤ 50 字，中文，无句号
- [ ] body 已说明「为什么 + 怎么改 + 影响面」
- [ ] 有破坏性变更时已标注 `BREAKING CHANGE`
- [ ] 未混入无关改动
