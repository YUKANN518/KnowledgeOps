# Security model

> **Phase 0 design archive:** this document contains planned controls for unimplemented AI and knowledge-base workflows. The [final architecture](ARCHITECTURE.md) describes the implemented authentication and authorization model.

## 信任边界与权限矩阵

所有授权由 Java 的用户状态、角色与资源事实计算，不能信任请求中的 userId/departmentId/role。管理员管理功能不能隐式读取知识正文。角色可组合，权限为满足资源条件的并集。

| 操作 | Employee | Support Agent | Knowledge Manager | Administrator |
| --- | --- | --- | --- | --- |
| 自己资料/会话/审批 | 自己 | 自己 | 自己 | 自己，不能读他人会话 |
| 知识检索、下载、引用 | 已授权 KB | 已授权 KB | 已授权 KB | 已授权 KB |
| KB 创建/成员 ACL | 无 | 无 | 无 | 可管理全部 |
| 文档上传/重试/归档 | 无 | 无 | 其 MANAGER KB | 其 MANAGER KB |
| 创建工单 | 自己 | 自己 | 自己 | 自己 |
| 读取/公开评论 | 自己工单 | 自己或本部门 | 自己工单 | 全部 |
| 内部评论 | 无 | 本部门 | 无 | 全部 |
| 分配工单 | 无 | 本部门 | 无 | 全部（目标仍需同部门） |
| 状态迁移 | requester 允许迁移 | Domain 状态机 | requester 允许迁移 | Domain 状态机 |
| 批准 AI 草稿 | 自己且动作有权 | 同左 | 同左 | 同左，不能代批 |
| 用户/部门/角色管理 | 无 | 无 | 无 | 全部 |
| 全局审计查询 | 无 | 无 | 无 | 全部，敏感字段脱敏 |

权限 code 固定为 `ticket.create`, `ticket.read.own`, `ticket.read.department`, `ticket.read.all`, `ticket.assign.department`, `ticket.assign.all`, `ticket.transition.own`, `ticket.transition.assigned`, `ticket.transition.all`, `ticket.comment.public`, `ticket.comment.internal`, `knowledge.read`, `knowledge.write`, `knowledge.admin`, `user.admin`, `audit.read`, `ai.use`。具体资源约束不可仅用注解角色替代。所有角色都有 create/read.own/public/knowledge.read/ai.use；其他按矩阵种入角色。

## 登录与令牌

- Spring Security 提供密码哈希，采用 BCrypt，初始 cost=12，实施时按本机耗时校准；不实现密码学。登录失败统一消息，禁止账号枚举。
- access JWT 10 分钟，签名密钥来自 Secret；校验 issuer/audience/exp/允许算法/sid。access token 仅在前端内存，角色 claim 不作为权限事实；Java 每次请求读取当前用户与角色。
- refresh token 随机 256-bit、不透明，7 天；仅 hash 入 MySQL，使用 HttpOnly / SameSite=Lax / Secure cookie（localhost 开发可配置 Secure=false）。每次刷新轮换，旧 token 保留 consumed_at 以发现重放；family_id 表示会话族。
- Redis 保存 `session:{sid}` 的 ACTIVE/REVOKED 状态，TTL 至 refresh family 到期；缺失一律无效。登录先持久化 refresh，再建立会话；失败不下发 token。登出、重放、停用账号撤销 family 和 session；Redis 失败时整个认证路径 fail closed。已撤销 token 即使 JWT 未过期也无效。
- 更新角色/部门/KB ACL 时同一事务递增 security_state.epoch；会话身份仍以当前数据库状态为准。
- 为避免MySQL/Redis撤销双写的恢复窗口，每次请求还检查MySQL中sid对应refresh family存在、未过期、没有revoked记录；Redis ACTIVE只是必要条件，不能覆盖数据库撤销。refresh/logout按user→refresh token固定顺序行锁串行化；先提交数据库撤销再撤Redis，后一步失败也不能复活已撤销family。登录只有两边就绪才返回token。
- CSRF：普通业务 API 必须 Authorization Bearer，不自动使用 cookie 鉴权。refresh/logout 使用 cookie，必须校验精确 Origin + 双提交 CSRF token（cookie/header，绑定 session）；SameSite 只是补充。CORS 默认同源，禁止凭据配通配符。仅限后端设置 refresh cookie path=/api/v1/auth。

## RAG 权限贯穿

Java 生成短期 opaque contextId，与 actor/session、conversation、turn、securityEpoch、expiresAt、允许工具范围绑定，存 Redis 60s，不发给模型。Java→Python 使用服务身份 + contextId；Python 调 `/internal/ai/contexts/{contextId}` 获得当前 `allowedKnowledgeBaseIds` 及精确 `(versionId,indexGeneration)` pairs。只有 Java 可创建上下文，浏览器不能传入 ACL。空集合直接拒答，禁止省略 filter 退回全库搜索。

Qdrant 查询 filter 必须是 `must(knowledge_base_id in allowedKBs, should(each versionId AND generation))`；pairs 不能拆成两个独立 in 集合。每个过滤字段建 keyword payload index。每次工具式 search 也重新获取 context。取得 IDs 后 `/internal/ai/evidence` 校验实际版本、权限与 epoch，再返回正文；没有授权的文本不能进入 prompt。Java 返回最终答案和加载历史会话时也重新校验 evidence，权限撤销时整体遮蔽相关 assistant message 文本，防止仅隐藏链接仍泄漏答案。v1 保守地在有一条引用失效时隐藏整条回答。

权限变化发生在处理途中：Java 在生成前 evidence fetch 与输出提交时比较 epoch，变化则返回 CONTEXT_STALE，Python 丢弃本轮上下文。不能追回撤权前已经发送给外部 LLM 或用户的资料。只有合成/获准的企业资料可以发外部 Provider，真实敏感资料的演示使用本地/Fake 模式。

## 工具与审批执行

工具注册表只有七项，输入 JSON Schema `additionalProperties=false`。身份字段不在模型输入中；assignTicketDraft 允许业务目标 `assigneeId`，Java 校验其部门和 Support 角色，它不是 actor。HTTP header 不能从模型生成。拒绝未知工具，不支持任意 URL/SQL/shell 工具。

Python 只提交草稿意图；Java 用 context 绑定 requester/conversation/turn/toolCallId，做首次鉴权与 schema 校验、存规范化 payload+SHA-256、目标 expectedVersion、expiresAt。审批详情必须展示将改变的字段、当前值、来源 AI、风险与失效时间，批准 API 必须携带 payloadHash。服务器重新计算并常量时间比较 hash；不能以聊天中的“好的”代替审批按钮的专用 API。

执行事务：锁 approval → 校验 APPROVED 未过期 → 校验 approved_by=requester、当前用户有效且权限满足 → 检查 payloadHash → 锁 ticket 并校验 expectedVersion → 业务写入 → 唯一 approval execution → audit → EXECUTED。无权限/版本变化返回 FAILED 并保存安全错误码；业务变更先回滚，再短事务记录 FAILED+execution+audit。重复执行返回已经保存的结果。竞争 worker 由行锁串行化。

批准本身不执行模型的新参数；执行 API 无 payload。Idempotency-Key 作用域为 actor+operation+key，body hash 不同为 409；业务结果、幂等记录同一事务保存，保留 24h。审批唯一执行约束永久保留，不能随幂等 TTL 删除。客户端直接写 API 同样校验权限与状态，不提供伪造 `origin=AI` 的入口。

幂等响应重放也必须重新鉴权：工单失去可见性返回404；问答/evidence必须重新检查全部引用，不能直接返回缓存的response_body绕过撤权。首次并发请求可先各自开展事务，末尾唯一幂等记录冲突的一方必须回滚整个业务事务，再读已提交结果；不得吞掉唯一键异常并提交额外业务效果。

草稿额外保存本轮证据依赖 `evidence_chunk_ids`（Java从检索/工具结果收集，不由模型声明），无证据时为空数组。查看、批准、执行时重检这些chunk的当前可读性；失权/归档则草稿不可继续执行。针对已有ticket的草稿还要有当前ticket读取权，否则详情404、列表排除。payloadHash覆盖tool名、规范化参数、expectedVersion与排序后的证据ID，避免只批准参数却置换前置条件。规范化使用UTF-8 JSON、对象key排序、无多余空格、数组顺序保留；工具参数没有浮点数，hash在Java生成并由客户端原样回传。

## 具体威胁与措施

| 风险 | v1 控制与负向测试 |
| --- | --- |
| IDOR | 每个 detail/list/count/download/citation 以同一资源策略过滤；无权限资源返回 404，禁止泄露存在性 |
| Prompt/tool injection | 文档和用户文本均不可信；证据与指令分隔；模型建议只有 schema+后端鉴权后才生效；测试资料含“忽略权限”也不能写入 |
| Unsafe upload / traversal | 允许扩展名+magic+MIME交叉检查；随机 storage key；拒绝用户路径/URL；resolve 后必须在私有根内；拒绝 symlink；20MiB/页数/解压上限 |
| Parser abuse | 非 root、资源/时间限制、拒绝加密/损坏 PDF、DOCX 外部关系不抓取；不执行宏；不自动下载文档引用 URL；纯图片 PDF 返回 OCR_REQUIRED |
| SQL injection | JPA 参数化，sort 列白名单，禁止拼接模型 SQL |
| XSS | Vue 自动转义、Markdown 禁止 raw HTML、链接协议白名单；不渲染模型产生脚本 |
| Provider secrets | 仅环境/secret，前端无变量暴露、日志字段白名单、异常清洗、gitignore，provider URL 仅运维配置 HTTPS allowlist |
| Broken audit | 业务写与审计同事务；app DB 用户无 audit UPDATE/DELETE；管理员 API 也不能改日志 |
| Rate limit | Redis 原子计数；对登录按 IP+账号、AI 按 user，429+Retry-After；不因 Redis 故障无限放行 |

不宣称数据库管理员不可篡改审计。v1 append-only 针对应用权限；外部不可变存储留给后续真实合规需求。会话/证据文本属于业务数据，不进入普通日志。演示环境只使用虚构资料；默认会话与执行详情 30 天清理，审计 90 天，保留 requestId/动作/资源标识但清除敏感摘要，清理由单独维护账号执行并写维护记录；被引用版本在相关消息保留期内不物理删除。
