# Architecture

## 服务边界与数据所有权

采用两个应用进程加一个 Java 模块化单体，避免把每个 Domain 拆成微服务。

| 组件 | 职责 / 拥有的数据 | 明确不能做 |
| --- | --- | --- |
| Vue | 交互、表单、展示引用与审批 diff | 决定身份、绕开 Java 调 Python、保存 API key |
| Java | 用户、RBAC、知识元数据、原文件、chunk 文本与来源、工单、会话、审批、审计、durable jobs | 实现 parser/embedding/RAG |
| Python | 无业务数据库的解析/RAG/有限 Agent；可重建 Qdrant 索引 | 直接连接 MySQL、自行授权、执行业务写操作 |
| MySQL | 唯一业务事实来源，事务、版本与历史 | 保存向量或 Provider 密钥 |
| Qdrant | chunk embedding + 过滤元数据，Python 独占访问 | 作为文档可见性或用户权限的事实来源 |
| Redis | 活跃/撤销会话、限流计数 | 持久化审批/任务唯一事实或通用全表缓存 |
| private volume | Java 写原文件；Python 只读挂载 | 作为 web 静态目录 |

Qdrant 不存 chunk 正文；只返回 chunk IDs。Python 用与本次请求绑定的授权上下文向 Java 获取证据正文，使向量索引可重建，权限判断集中，撤权后也不会依靠过期 payload 授权。

## Java package structure

Monorepo布局：

```text
KnowledgeOps-AI/                 # 当前工作目录 project2 即仓库根
  frontend/                     # Vue skeleton
  backend-java/                 # Java package skeleton
  ai-service/                   # Python package skeleton
  docs/ADR/                     # 六项决定与取舍
  docs/contracts/               # 三份OpenAPI与工具JSON Schema
  docs/database/                # SQL与完整ERD
  evaluation/                   # 合成语料、40个问题、结果目录
  infra/                        # 应用Compose拓扑与后续CI模板
  scripts/                      # 架构契约生成与静态验证
  .github/workflows/            # 当前可执行的架构CI
  README.md
  docker-compose.yml
  .env.example
  ARCHITECTURE_PHASE_REPORT.md
```

根包 `com.knowledgeops` 下 `auth`, `user`, `knowledge`, `ticket`, `ai`, `audit`, `shared`。模块内分 `api`（Controller/DTO）、`application`（用例、事务、权限）、`domain`（状态/不变量）、`infrastructure`（JPA/外部 client）。Repository 接口放 domain，JPA 适配放 infrastructure。不为每个实体制造接口或抽象工厂。

- Controller 只做输入校验、认证信息提取与调用用例；禁止跨模块直连 repository。
- ticket 应用服务统一实现手动和审批执行路径；区别只在可信 actor/origin 上下文。
- ai 调 ticket 的公开应用接口；audit 暴露 append API，参与调用方事务。
- knowledge 负责 job lease、版本发布与 chunk manifest 持久化；不解析原文。
- shared 仅放错误、requestId、时间/ID、分页；禁止放业务 Service。
- JPA 为首版唯一 ORM；Flyway 管 schema。暂不使用 MyBatis、Spring AI、LangChain 或 SQLAlchemy。

## Python package structure

`app/api` 路由；`core` 配置/鉴权/错误/日志；`providers` LLM+Embedding；`rag` parser/normalizer/chunker/retriever/citation；`agents` 有限步骤 planner；`tools` Java 内部 client 与 schema 注册；`schemas` Pydantic；`services` 编排用例；`evaluation` 评测适配；顶层 `tests`。

路由只接认证后的内部请求。禁止从模型输出构造 URL、SQL、身份字段或任意 HTTP 请求。所有返回 DTO 严格拒绝额外字段。Python 仅持有 Java 内部服务凭据和 Qdrant/Provider 配置，不持有 MySQL/Redis 密码。

## 通信、超时与异步处理

Java→Python REST，Python→Java 受控内部 REST。服务凭据分方向，环境注入；仅内网监听，部署时使用 TLS。请求头贯穿 `X-Request-ID`。模型无法看到或修改内部凭据。

无需 Celery/Redis Queue：Java 在保存上传版本的同一事务插入 `document_jobs`，由单个定时 worker 用短事务 `SELECT ... FOR UPDATE SKIP LOCKED` 领取任务。数据库记录 attempts、available_at、lease_until、lease_token；领取后立即提交，再做网络调用，绝不持锁跑 parser。

`POST /documents/process` 为**内部同步、有限时**处理：Java worker 等待最多 130s，Python 120s 截止，lease 180s；到期可重新领取，同一 attempt 的 lease token 防旧响应覆盖。最多 3 次，延迟 5/30/120s 加抖动，永久错误不重试。Java 崩溃后任务仍在 MySQL，Python 崩溃不会丢 job。v1 worker 并发 1；任务吞吐不足时再评估独立队列。

每次 attempt 用新 index generation；Python 先写全量向量，返回 chunk manifest。Java 检查 lease token 和文件 hash 后在一次事务持久化 chunks、置版本 READY、更新 document.current_version_id、任务成功与审计。查询只使用 Java 当前 READY version/generation 列表，所以未发布或失败的向量永远不进入 retrieval。旧 attempt 迟到返回 409；孤儿 generation 在下次重试/定期清理时删除，清理不能删除当前或历史证据所用的 chunk 正文。

问答 Java 总预算 35s，Python 30s；内部普通 API 3s，Provider 单次 15s；只对明确 429/503 且剩余预算充足重试 1 次。会话和 tool-result 不盲目重放；关联 turn/call IDs。Python 不直接把生成内容 SSE 发给浏览器，Java 最终校验后一次性返回。

发布新current、归档或ACL变化都在同一事务递增security_epoch，防止在途查询提交过时可见性结果。无progress callback的首版job.step只持久化Java实际观察到的阶段（VALIDATE/PARSE/COMMIT）；Python内部阶段写带jobId的结构化日志，失败响应details给出最后stage。EMBED/INDEX等枚举预留给后续需要时的进度回报，不伪造实时百分比。

## 一致性与恢复

审批业务写入只涉及一个 MySQL 本地事务：锁 approval → 再鉴权 → 乐观版本校验 → ticket/history/comment → execution → audit → EXECUTED。不存在远程写调用，因此无需分布式事务。详情见 [安全模型](SECURITY_MODEL.md)。

文档索引采用“先准备索引，再发布指针”，不是 MySQL 与 Qdrant 双写强一致。索引是派生数据；备份 MySQL+原文件可以重建。首版不缓存 ACL；每次检索前向 Java 取新快照，生成前与返回前校验全局 security_epoch；变化即丢弃输出，重新开始或返回 409 CONTEXT_STALE。撤权线性化于 Java 最终校验：在校验前提交的撤权必须生效，不承诺追回此前已返回资料。

## Docker / observability

详见 [infra/README.md](../infra/README.md)。仅 frontend 发布宿主端口；Java 在前端反代后，Python/数据服务不发布端口。应用 overlay 把 Java 放入业务数据网，Python 放入 AI 数据网，两者另有服务通信网。Python 无 MySQL 网络路径和账号。

JSON 日志字段：timestamp、level、service、requestId、route、status、durationMs；AI 另记 provider/model、prompt/completion token 数、retrieval/LLM latency、toolCount、errorCode。禁止正文 prompt、访问令牌和原始文件日志。Prometheus 格式基础指标由 Java actuator 与 Python metrics 暴露在内网；不以 userId/requestId 作指标标签。存活检查只验证进程，ready 检查必需依赖；Provider 失败显示 degraded，不让整个工单系统不健康。
