# KnowledgeOps interview guide

## Q1. KnowledgeOps 是什么？

**30 秒回答：** KnowledgeOps 是一个面向内部团队的轻量工单与知识管理系统。我用 Vue 3 实现操作界面，用 Spring Boot 模块化单体承载身份、权限、工单、文章、文档和审计，MySQL 保存业务数据，Redis 保存活动会话状态。项目重点是展示可运行的 Java 后端工程能力，包括 JWT 与 Refresh Token Rotation、RBAC、资源级授权、事务、乐观锁、Flyway、安全文件处理、Testcontainers 和 Docker Compose。

## Q2. 为什么做这个项目？

我希望练习一个比单表 CRUD 更接近真实开发的闭环：用户登录后有不同权限，资源还有所属范围；多个写操作需要事务和并发控制；上传文件需要安全边界；最后还要能测试并通过容器运行。工单和知识管理的业务足够直观，面试时可以把技术决策和可见行为对应起来。

## Q3. 系统架构是什么？

浏览器访问 Vue SPA，生产构建由 Nginx 提供，Nginx 将 `/api` 反向代理到 Spring Boot。Java 服务是模块化单体，包含 auth、user、ticket、knowledge、audit 和 shared 模块。MySQL 是业务事实来源，Redis 只保存活动会话状态，私有 Docker volume 保存上传文件。默认 Compose 一共运行 frontend、java-backend、mysql、redis 四个服务。

## Q4. Authentication 怎么实现？

登录时后端用 BCrypt 校验密码，创建短期 JWT access token 和随机 opaque refresh token。Refresh token 只把 SHA-256 hash 写入 MySQL，原值放 HttpOnly cookie；会话 family 同时以 TTL 写入 Redis。受保护请求需要 JWT 有效、用户仍为 ACTIVE、Redis 会话存在、MySQL 中仍有有效 refresh family。Refresh 会锁定并消费旧 token、生成新 token；Logout 同时撤销数据库 family 和 Redis session。

## Q5. 为什么 Access Token 和 Refresh Token 分开？

Access token 生命周期短，适合每次 API 请求快速验证；Refresh token 生命周期长但暴露面更小，只通过 HttpOnly cookie 发送，并且在数据库中可轮换和撤销。这样前端不需要长期保存 bearer token，同时登出、重放检测和账号停用可以让会话尽快失效。

## Q6. RBAC 怎么做？

数据模型是 `User → Role → Permission`，角色和权限 code 由 Flyway 种入。JWT 不作为权限事实，过滤器每次从当前用户关系加载角色和权限，转换为 Spring Security authority。Controller 的 `@PreAuthorize` 做动作级检查，Service 再检查具体资源范围。

## Q7. 什么是 Resource-level Authorization？

RBAC 只能说明某个角色通常能做什么，资源级授权还要判断“这一个资源是否属于调用者可访问范围”。例如 Employee 有 `ticket.read.own`，但查询 ticket UUID 时仍必须判断 `creatorId` 是否等于当前用户。Support Agent 则按部门读取，并且只能流转分配给自己的工单。

## Q8. 如何避免 IDOR？

后端不会因为 ID 合法就直接返回实体。Ticket Service 先加载资源，再结合当前用户 ID、部门和权限检查范围，不满足时返回 403。列表查询也使用与角色对应的 repository 查询，避免先返回过宽数据再由前端过滤。集成测试覆盖普通员工读取他人工单必须 403。

## Q9. 为什么使用 Flyway？

数据库结构需要和代码一起版本化、可重复创建和可审查。Flyway 按 `V1` 身份认证、`V2` 工单、`V3` 知识管理依次建表和种入固定权限。Hibernate 设置为 `ddl-auto=validate`，只验证映射，不在运行时偷偷修改 schema。

## Q10. 为什么使用 Redis？

Redis 的真实用途是活动会话注册表，不是通用缓存。登录和刷新会写入带 TTL 的 `session:{familyId}`，每次 JWT 请求都会检查它。Logout 删除 key，Redis 缺失或不可用时认证 fail closed。Refresh token 的持久证据和 hash 仍在 MySQL。

## Q11. 什么地方使用 Transaction？

Ticket 的创建、评论、分配、状态流转都与对应审计事件在同一个事务中；文章创建、更新、发布、归档和文档元数据变更也一样。Refresh rotation 使用行锁消费旧 token 并创建新 token。文档上传还注册事务回滚回调：数据库提交失败时删除刚写入的文件。

## Q12. 为什么使用 Optimistic Lock？

工单和文章可能被两个页面同时修改。如果只执行最后写入，会无声覆盖先前修改。实体使用 JPA `@Version`，请求携带 `expectedVersion`；版本不一致时返回 409，让客户端刷新后再决定，而不是丢失更新。User 也带 version 保护管理操作。

## Q13. 文件上传怎么保证安全？

后端限制 20 MiB，并检查原始文件名、允许扩展名、声明 MIME、PDF/DOCX 基础文件签名和文本 UTF-8 合法性。落盘时生成 UUID 文件名，只允许解析到配置的 storage root 直属路径，并拒绝符号链接。API 只返回原文件名和业务元数据，不暴露 storage key 或本机路径；下载也必须再次鉴权。

## Q14. 为什么不用 MinIO？

项目是单机作品集和面试 Demo，本地持久化 volume 已满足容量和可操作性目标。引入 MinIO 会增加部署、凭据和一致性处理，但当前没有相应规模收益。如果进入多实例或云部署，再把 `FileStorageService` 换成对象存储实现更合理。

## Q15. Testcontainers 有什么作用？

它让集成测试运行真实 MySQL 8.4 和 Redis 7.4，验证 Flyway、数据库约束、JPA 行为、Redis session 和完整 HTTP 安全链路。相比 H2 或 mock Redis，它能更早发现生产数据库方言、约束和依赖行为差异。Phase 1–3 的 17 个集成测试场景都使用这套方式。

## Q16. Audit Log 怎么实现？

业务 Service 调用 `AuditService.record` 写 append-only `audit_logs`。记录包含 actor、action、resource、requestId、时间和小型 allowlist metadata。Request ID 来自过滤器。敏感字段和正文不会放入 metadata；登录、刷新、登出、角色变更、工单动作和知识动作都有对应枚举。关键业务写与审计处于同一事务。

## Q17. Refresh Token Rotation 是什么？

每次刷新都把当前 token 标为 consumed，并在同一 family 下生成新的随机 token。旧 token 再次出现时视为 replay，后端撤销整个 family并删除 Redis session。数据库查询对 token hash 加锁，避免两个并发刷新都成功。前端则用 single-flight 合并同时出现的 401，减少正常客户端制造并发 refresh 的机会。

## Q18. 前端权限控制安全吗？

前端权限控制本身不是安全边界。它根据 `/users/me` 返回的 permissions 隐藏菜单、路由和按钮，让交互更清楚；用户仍可绕过浏览器直接发请求。因此 Controller 的 authority 检查和 Service 的资源授权必须独立执行，真实 403 smoke test 证明后端会拒绝越权请求。

## Q19. 项目中遇到过哪些真实问题？

1. **Windows 文件复制问题：** `Files.copy` 使用 `COPY_ATTRIBUTES` 时在 Windows 临时文件系统不受支持。最小修复是去掉该选项，不改变文件内容和安全检查；随后重跑 Phase 3 全部测试和 runtime smoke。
2. **Vue 表单引用问题：** Element Plus form ref 被写成普通变量，浏览器中 submit validation 没有执行。修复为 Vue `ref<FormInstance>()` 并通过真实登录、工单和文章表单流程验证。
3. **JDK/Formatter 环境问题：** 本机 JDK 25 可以跑完所有测试，但旧 google-java-format 在 verify 最后阶段与 JDK internals 不兼容；JDK 21 Linux 容器又会看到 Windows bind mount 的 CRLF。没有为通过工具而修改稳定业务代码，报告明确区分“测试通过”和“完整 lifecycle 未通过”。

## Q20. 如果成为真实生产系统，会继续做什么？

我会先根据使用规模和故障数据评估对象存储、集中日志与指标、备份恢复、密钥轮换、自动化发布和生产部署。只有知识量和检索需求证明必要时才引入全文搜索。当前没有实现这些能力，因为项目目标是把身份、授权、事务、并发、文件安全和测试闭环做完整，而不是累积技术名词。
