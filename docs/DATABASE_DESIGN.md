# Database design

> **Phase 0 design archive:** this planned schema is broader than the implemented Flyway V1–V3 schema. See the [final architecture](ARCHITECTURE.md) and executable migrations for delivered tables.

业务持久化只有 MySQL 8.4；完整字段、类型、PK/FK/Unique/Index/CHECK 以 [schema.sql](database/schema.sql) 为准，本轮不自动执行 DDL。Qdrant 保存可重建向量，Redis 保存有 TTL 的会话与限流状态。共 25 张业务/关联/技术表（实际数量由验证脚本核对）。

## 约定与不变量

UUID 用 CHAR(36) 便于作品集调试，未来有实测存储压力再转 BINARY(16)。UUID 由可信应用生成，ticket_number 为 `KO-`+ULID（不依赖 MySQL 自增计数器），UTC DATETIME(6) 在 API 转 ISO8601。字符串 utf8mb4，用户名统一小写；token hash 和幂等 key 大小写敏感。

可修改记录有 created_at/updated_at 和必要 optimistic version；事件/历史只保留创建时间。静态 role/permission 不机械添加 timestamps。所有 FK 默认 RESTRICT，避免级联擦除证据；v1 不 hard delete 用户/工单/知识。用户 DISABLED，KB/Document ARCHIVED；无统一 deleted_at。

数据库约束可保证引用存在、唯一版本号、同 document 的 current pointer、一个 approval 至多一个正式 execution。应用事务负责角色/部门范围、状态机、current version 必须 READY、每文档最多一个在途版本、审批 payload hash 与版本前置条件、comment长度等跨行业务规则。不能假设 FK 代替授权。

文档当前版本复合 FK `(id,current_version_id) → document_versions(document_id,id)` 解决指向别的文档版本的问题；READY 条件在持有 document 行锁的发布事务中检查。删除/归档会递增 security_epoch；归档不删除 chunks。citation 从 chunk 追溯 version→document→KB，避免重复存可变的 ACL。

审批对 target ticket 的 expected_version 对应 JPA @Version。创建工单无 target；comment/assign/status 都带版本。评论追加也递增 ticket.version，采取保守冲突策略。AI 草稿创建时 Java读取并绑定期望版本，模型不能指定或降低版本。

## 数据字典索引

审批的evidence_chunk_ids是服务器生成的有限证据依赖数组（最多24项），用于批准/执行时重新鉴权；JSON中的ID由应用检查存在性，不伪称有数据库FK。任何被会话或审批引用的chunk在保留期限内不得物理清除，维护清理也须检查这些依赖。

| 分组 | 表 | 目的 |
| --- | --- | --- |
| Identity | departments, users, roles, permissions, user_roles, role_permissions | 角色与资源范围基础 |
| Security | security_state, refresh_tokens | ACL/身份 epoch 与 refresh family 轮换证据 |
| Knowledge | knowledge_bases, knowledge_base_members, documents, document_versions, document_chunks, document_jobs | 授权、不可变来源、可重建索引任务 |
| Ticket | tickets, ticket_comments, ticket_assignments, ticket_status_history | 当前状态与追加历史 |
| AI | ai_conversations, ai_messages, ai_message_citations, ai_approvals, ai_tool_executions | 对话证据、人类决定与执行分开 |
| Infrastructure | idempotency_records, audit_logs | 请求重放与行为追溯 |

## 关键查询与索引

| 查询 | 对应索引 / 条件 |
| --- | --- |
| 我的有效 KB | knowledge_base_members(user_id,knowledge_base_id)，join ACTIVE KB |
| KB 下当前文档 | documents(knowledge_base_id,status,updated_at,id)，join current_version |
| 某版本 chunks | UNIQUE(version_id,ordinal)，ordinal排序 |
| 待领取任务/过期租约 | document_jobs(status,available_at,id) / (status,lease_until) |
| 员工工单按状态 | tickets(requester_id,status,created_at,id) |
| 部门工单按状态 | tickets(department_id,status,created_at,id) |
| 我的处理队列 | tickets(assignee_id,status,updated_at,id) |
| 工单评论 | ticket_comments(ticket_id,visibility,created_at,id) |
| 我的审批/恢复 | ai_approvals(requester_id,status,created_at,id) / (status,expires_at,id) |
| 会话列表/消息 | owner_id,updated_at,id / conversation_id,created_at,id |
| 审计追踪 | audit_logs(request_id,timestamp)；actor / resource / time 三种索引 |

工单不指定 status 的小数据列表可能额外 filesort，Phase2用 EXPLAIN ANALYZE 测量后决定补 `(requester_id,created_at,id)` 等索引；不假装现有复合索引覆盖所有排序。默认分页 page=0,size=20,max=100；排序字段固定白名单并追加 id 确定顺序，最大 page=1000，深页导出不在 v1。

## 事务边界 / 索引状态 / 数据保留

上传写文件→事务版本+job+audit；文件写好但事务失败的孤儿由定期清理扫描，禁止发布失败文件。Qdrant prepare→事务 chunks+READY/current+job+audit。分配写 ticket+assignment+audit，迁移写 ticket+history+audit，批准写 approval+audit，执行写业务+execution+approval+audit；关键审计失败全部回滚。

Qdrant collection 使用 cosine，dimension=512；payload 为 chunk_id、knowledge_base_id、document_id、version_id、index_generation、embedding_revision，过滤字段设 keyword index。原文只存 MySQL chunks。没有分布式 join：Java 提供有限可见 version/generation pairs（首版最多1000 current文档），Python构建过滤；超预算返回 SCOPE_TOO_LARGE，不静默放宽过滤。

MySQL backup + 私有文件 volume backup 同一维护窗口；恢复后重新索引。Redis 丢失让用户重新登录，绝不能恢复成“全部有效”；Qdrant丢失标记检索不可用并重建。审计/会话保留策略见 SECURITY_MODEL，物理清理属于维护操作，须先清 citation/消息后清不再被引用的历史 chunk/source，不由业务 API 随意级联删除。

完整 ERD 见 [ERD.md](database/ERD.md)，关系从 SQL FK 生成；`security_state` 无关系、独立单例。
