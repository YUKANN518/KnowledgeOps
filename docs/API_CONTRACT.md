# API contract

> **Phase 0 design archive:** this contract contains proposed endpoints beyond the delivered product. The implemented controllers and the runtime OpenAPI UI are authoritative for Phase 5.

本文件解释业务语义，字段/required/enum/响应类型以 [contracts/](contracts/README.md) 下 OpenAPI 3.1 为准。契约为草案、尚未部署。修改用 `scripts/build_contracts.py` 更新再生成 JSON，不能仅修改生成文件。

## 通用约定

- Java browser API `/api/v1`，内部 `/internal/ai`；Python 根路径仅内网。JSON 字段 camelCase、数据库 snake_case，工具返回 `draftId` 即业务描述中的 draft_id。
- ID 为 UUID；时间 UTC RFC3339；request header `X-Request-ID` 可选，服务校验长度/字符（只允许 UUID 或 `[A-Za-z0-9_-]{1,64}`），缺失或非法则新建，并回传。header 不赋予权限。
- 业务写、审批、问答采用必填 `Idempotency-Key`，长度≤100；同 actor+operation+key+same body 重放返回原响应，不同 body 409。上传 hash 包括文件 SHA256 和元数据，不含 multipart 随机 boundary；同 conversation+turnId 唯一，换 key 也不能重复生成。
- 重放前重新做资源/evidence鉴权，已经撤权的历史响应不能从幂等缓存取回。审批payloadHash还覆盖expectedVersion和服务器收集的证据依赖，详情返回evidenceChunkIds供审查。
- 通用列表：`page=0,size=20`，size≤100、page≤1000，`sort=createdAt,desc|asc`，稳定追加 id 同方向排序。没有 createdAt 的静态资源（部门、成员）按稳定主键排序，忽略 createdAt排序请求并返回422，实施时为其使用特定 sort 枚举；见当前契约的专用覆盖。total 必须在同一 ACL 范围内统计。过滤不增加权限，不接受 SQL 字段表达式。
- GET detail 不可访问时 404；已知允许资源上的动作无权限 403；无身份401；schema/domain字段不合法422；JSON语法错误400；版本/状态/重复冲突409；限流429+Retry-After；依赖不可用503。
- 乐观并发控制：业务更新 DTO 的 expectedVersion，不同时叠加 If-Match；冲突响应不暴露无权资源当前版本。审批 expectedVersion 由 Java 草稿生成时读取绑定，不能让模型输入。
- 所有输入对象 `additionalProperties=false`，Bean Validation/Pydantic 不自动忽略未知身份字段。限制字符串长度、文件大小与数组数量。

错误示例：

```json
{"code":"VERSION_CONFLICT","message":"工单已更新，请重新查看后操作","details":[],"requestId":"c799ab97-08df-490f-9a2e-ef5c3c909d31","timestamp":"2026-09-20T06:00:00Z"}
```

| code family | HTTP | 客户端处理 |
| --- | --- | --- |
| VALIDATION_ERROR / INVALID_TOOL_ARGUMENTS | 422 | 高亮字段，不自动重试 |
| AUTHENTICATION_REQUIRED / TOKEN_REVOKED | 401 | 尝试一次刷新或登录 |
| FORBIDDEN / CSRF_REJECTED | 403 | 显示权限限制 |
| RESOURCE_NOT_FOUND | 404 | 不区分不存在与不可见 |
| VERSION_CONFLICT / INVALID_TRANSITION / IDEMPOTENCY_CONFLICT / CONTEXT_STALE | 409 | 重新获取，不偷偷改变待执行内容 |
| PAYLOAD_TOO_LARGE | 413 | 缩减上传/请求 |
| AI_PROVIDER_UNAVAILABLE / VECTOR_STORE_UNAVAILABLE / AUTH_DEPENDENCY_UNAVAILABLE | 503 | 展示可重试状态，保留手动工单入口 |
| DOCUMENT_PARSE_FAILED / OCR_REQUIRED / DOCUMENT_ENCRYPTED | job FAILED | 由 job/version详情返回错误码，不把异步失败伪装上传HTTP失败 |

## Java public operations

| Resource | APIs | 授权/语义 |
| --- | --- | --- |
| Auth | POST /auth/login, /refresh, /logout | login返回短期access；refresh/logout cookie+CSRF；刷新轮换 |
| Users | GET /users/me；GET/POST /users；GET/PATCH /users/{id}；GET /roles | me自己；其余Administrator；密码不出响应 |
| Departments | GET/POST /departments；GET/PATCH /departments/{id} | Administrator；存在活动用户不能停用部门 |
| KB | GET/POST /knowledge-bases；GET/PATCH /knowledge-bases/{id} | 列表/详情按ACL（管理员可看管理元数据）；写由Administrator |
| ACL | GET /knowledge-bases/{id}/members；PUT/DELETE .../members/{userId} | Administrator；变化递增epoch；MANAGER授予时校验角色 |
| Documents | GET/POST /documents；GET/PATCH /documents/{id} | 列表/详情按ACL，写由MANAGER；multipart上传202 |
| Versions | GET/POST /documents/{id}/versions；GET .../{versionId}/content；POST .../{versionId}/retry | 版本不可覆盖；下载鉴权，重试仅FAILED |
| Jobs | GET /document-jobs/{id} | 有管理权才能看错误详情；前端2s/5s/10s退避轮询 |
| Search | POST /search | 只检索；无证据返回 evidence=[]；不调用LLM |
| Conversations | GET/POST /ai/conversations；GET /ai/conversations/{id}/messages | 仅owner；证据失权遮蔽旧答案 |
| Query | POST /ai/query | 已存在自己的conversation；同turnId幂等，35s内返回完整回答 |
| Assistant | POST /ai/assistant | 有限tool loop，最多一个write draft，不能宣称已执行 |
| Drafts | POST /ai/tool-requests | 只接受本轮已经持久化的模型提议，call ID/参数必须一致；主要供Java编排调用 |
| Approvals | GET /ai/approvals；GET /ai/approvals/{id}；POST .../decision, .../execute | 自己确认自己的权限内动作；execute body为空 |
| Tickets | GET/POST /tickets；GET /tickets/{id} | 列表按owner/department/admin资源策略 |
| Assignees | GET /tickets/{id}/assignee-candidates | 有分配权者可读同部门有效Support候选，无密码/额外个人信息 |
| Comments | GET/POST /tickets/{id}/comments | PUBLIC对参与者；INTERNAL仅Support/Admin；追加不可编辑 |
| Assignments | GET/POST /tickets/{id}/assignments | 当前状态允许且目标同部门Support；历史不可改 |
| States | POST /tickets/{id}/transitions；GET .../status-history | 明确状态机，不提供任意status PATCH |
| Audit | GET /audit-logs | Administrator；按actor/action/resource/time/requestId过滤 |

上传成功返回 documentId/versionId/jobId；不是 READY 承诺。multipart幂等重试必须校验文件 hash。不接受 storageKey/path/url 字段，Java自行生成路径。下载响应 `Content-Disposition: attachment`、`X-Content-Type-Options: nosniff`；citation sourceUrl 指向版本 content API，前端带 bearer fetch 后打开安全预览，不依赖浏览器裸链接附带access token。

## Java ↔ Python contract

| 调用 | 接口 | 结果/约束 |
| --- | --- | --- |
| Java → Python | POST /documents/process | jobId/leaseToken/versionId/generation/storageKey/hash；同步返回完整manifest；Java持久化发布 |
| Java → Python | POST /rag/query | opaque contextId+question；Python自行回Java拿授权，返回answer/claims/citations/usage/epoch |
| Java → Python | POST /rag/search | 同过滤规则，返回检索证据，无模型生成 |
| Java → Python | POST /agent/plan | 一步FINAL或TOOL；不直接执行业务 |
| Java → Python | POST /agent/tool-result | context+turn+call+完整有限历史，继续一步；校验服务身份 |
| Java → Python | GET /health | 进程存活，不暴露环境信息 |
| Python → Java | GET /internal/ai/contexts/{contextId} | 验证服务身份、session、epoch、期限，输出授权范围 |
| Python → Java | POST /internal/ai/evidence | 校验本次context下所有chunk；任一非法整批拒绝，不先返回其他文本 |
| Python → Java | POST /internal/ai/tools/read | 仅getTicket/listTickets；每次重做资源授权 |
| Python → Java | POST /internal/ai/tool-requests | 只建PENDING草稿；无 approve/execute 内部API |

服务请求使用不同方向的 X-Service-Token；用户 JWT 不转发给 Python。Java 公开connector 8080拒绝 `/internal/**`；internal connector 8081接受服务身份，不接受browser JWT。前端代理只转发 /api/，禁止 internal 和 actuator。文档任务不依赖短期用户context，服务身份+已存在的job/lease/source hash绑定操作；任务只做索引，不能改变ACL。

可选knowledgeBaseIds仅用于缩小范围：Java与当前可读集合取交集，再把结果绑定context；Python必须继续与context求交集，不接受客户端扩大范围。交集为空立即返回无证据，超出首版1000个current版本预算返回422 SCOPE_TOO_LARGE，不允许降级到无过滤检索。

## Tool request / approval example

```json
{"name":"createTicketDraft","arguments":{"title":"VPN连接失败","description":"已按指南重连但错误仍存在","category":"IT","priority":"NORMAL"}}
```

Java 注入 actor、department、conversation、expectedVersion，返回 `draftId,payloadHash,expiresAt,status=PENDING`。AI schema 中没有 requesterId/userId/departmentId/adminRole。assign 的 assigneeId 是被分配人且须Java验证，不是调用身份。

`POST /ai/approvals/{id}/decision` body 为 `{decision:"APPROVE",payloadHash:"..."}`；批准响应 APPROVED。`POST .../execute` 无 body；成功响应 EXECUTED+executionResult；重复返回原结果。未批准409，过期更新EXPIRED并409，版本/权限失败更新FAILED并返回409/403；用户无权查看该approval则404。worker恢复执行同一应用方法，不开放worker身份给浏览器。

## Audit event catalogue

| 事件 | actor / metadata白名单 |
| --- | --- |
| AUTH_LOGIN / AUTH_LOGOUT / AUTH_REFRESH_REPLAY | user或ANONYMOUS；结果、脱敏IP/账号hash，不存密码/token |
| DOCUMENT_UPLOAD / DOCUMENT_READY / DOCUMENT_FAILED / DOCUMENT_ARCHIVE | 人类/worker；版本ID、hash、文件类型、状态/错误码 |
| KNOWLEDGE_QUERY | 发问人；KB IDs、命中数、用量/耗时/结果，禁止完整prompt |
| TICKET_CREATE / TICKET_COMMENT / TICKET_ASSIGN / TICKET_TRANSITION | 手动人或approved_by；目标ID、状态、版本、origin，reason脱敏摘要 |
| AI_TOOL_REQUEST / AI_APPROVAL_DECISION / AI_APPROVAL_EXPIRED / AI_EXECUTION | requester/approver/worker；callId、approvalId、toolName、hash、错误/结果ID |
| ADMIN_USER_CHANGE / ADMIN_ROLE_CHANGE / ADMIN_KB_ACL_CHANGE / ADMIN_DEPARTMENT_CHANGE | 管理员；目标ID、字段名、epoch，不存密码 |

写操作事件与业务同事务；只读成功/拒绝事件独立短事务。读取审计写失败时返回 AUDIT_UNAVAILABLE 503（不返回资料），安全边界失败保留结构化运行错误，不打印payload。requestId 连接 HTTP/AI/worker，worker另带jobId；审批决策与执行可有不同requestId，但共享approvalId/toolCallId。
