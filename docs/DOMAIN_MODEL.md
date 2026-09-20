# Domain model and invariants

| Domain / aggregate | 核心规则 |
| --- | --- |
| User / Department | 用户停用不删除历史；角色多选、主部门一个；调岗不改变旧单所属部门 |
| Role / Permission | 固定角色和权限 code；管理员分配角色，不在 v1 做可编程策略系统 |
| KnowledgeBase / Membership | 显式用户 ACL；MANAGER 包含读权限且需 Knowledge Manager 或 Administrator 管理能力 |
| Document | 所属知识库固定，display title 可改；current_version 指向同一 document 的 READY 版本 |
| DocumentVersion | immutable source、连续 version_no、hash、parser/chunker/embedding revision；READY 内容不原位更新 |
| DocumentChunk | 从属于成功版本，保存正文、hash、页码/章节/offset 与 index generation；Python 生成，Java 保存 |
| Ticket | requester、归属部门、当前 assignee/status、version；编号单独唯一 |
| TicketComment | 追加，作者从可信 context 注入；员工只能看到 PUBLIC 评论 |
| TicketAssignment | 追加事件，from/to assignee 与操作者；当前 assignee 冗余在 Ticket，事务内同步 |
| TicketStatusHistory | from/to、reason、actor、origin；创建时 from=NULL→OPEN 也记一条 |
| AIConversation / AIMessage | Java 拥有；一个会话只有其 owner 可读；assistant message 保存经验证 evidence 引用 |
| AIToolExecution | 一次工具请求/执行结果；读取和草稿也可记录，正式写执行有唯一 approval_id |
| AIApproval | 不可变的规范化 tool_payload、hash、requester、版本前置条件、有效期和决策 |
| AuditLog | append-only 的行为记录，不等同领域状态历史或普通运行日志 |

技术辅助表：refresh_tokens、document_jobs、idempotency_records、security_state、ai_message_citations、角色和成员关联。没有独立 TicketDraft 表：AIApproval 的 PENDING 记录就是草稿。没有独立 Python DB 或 vector SQL 表。

## Ticket 状态机

角色缩写：E=单据 requester；S=本部门 Support Agent；A=Administrator（可管理全局工单）。S 执行进展/解决时必须为当前 assignee；分配可由同部门任意 S/A 做。E 如果兼任 S，只能以满足对应动作条件的身份执行。

| From | To | 执行者 | 必须填写 reason | 前置条件 |
| --- | --- | --- | --- | --- |
| OPEN | IN_PROGRESS | 当前 assignee S / A | 否 | 已分配有效、同部门 S |
| OPEN | CANCELLED | E / A | 是 | 无 |
| IN_PROGRESS | WAITING_USER | 当前 assignee S / A | 是 | 说明需要员工补充什么 |
| WAITING_USER | IN_PROGRESS | 当前 assignee S / A | 否 | 员工评论不会自动触发迁移 |
| IN_PROGRESS | RESOLVED | 当前 assignee S / A | 是 | 解决说明 |
| WAITING_USER | RESOLVED | 当前 assignee S / A | 是 | 解决说明 |
| RESOLVED | IN_PROGRESS | E / 当前 assignee S / A | 是 | 解决方案无效 |
| RESOLVED | CLOSED | E / A | 否 | 无自动关闭计时器 |
| IN_PROGRESS / WAITING_USER | CANCELLED | A | 是 | 特殊中止 |

其他迁移全部 409 INVALID_TRANSITION，CLOSED/CANCELLED 为终态。PUBLIC 评论可追加于所有状态用于澄清；INTERNAL 评论仅 S/A。分配仅 OPEN/IN_PROGRESS/WAITING_USER，目标必须是有效同部门 Support Agent；assignee 变化本身不改变 status。用户调岗/停用前管理员须处理其非终态分配，存在分配则返回 409 ACTIVE_ASSIGNMENTS，避免悬空处理人。

AI 可建议表内所有迁移，但 requester 必须具有相同行为权限；AI **没有直接状态迁移权**。已审批执行仍需通过同一状态机。审批不授予权限。手动操作由明确 UI 点击提交，无须额外创建 AIApproval。

## 文档生命周期与版本

Document 为 ACTIVE/ARCHIVED；DocumentVersion 为 UPLOADED→PROCESSING→READY 或 FAILED。Job step 为 VALIDATE/PARSE/NORMALIZE/CHUNK/EMBED/INDEX/COMMIT，Job status 为 QUEUED/RUNNING/SUCCEEDED/FAILED。两者分开可表达业务可用性与技术执行进度，不把 ARCHIVED 混进每个处理步骤。

- 上传先做大小/MIME/格式校验，失败不生成可处理版本；通过后原文件不可变落盘并创建 UPLOADED + QUEUED。
- 同一 document 同时只允许一个未完成版本；以 document 行锁检查；新增版本不覆盖旧文件。版本号 `UNIQUE(document_id, version_no)`。
- PROCESSING 期间旧 current READY 可继续使用；成功才原子切换 current pointer。首次失败则无 current 版本。
- 重试只能 FAILED，由知识库 MANAGER 发起；重新入队、attempt 累加、generation 更新；最多 3 次自动重试，人工重试新建 job 并记录审计。
- READY 版本禁止重解析原地更新；模型/切块策略更换创建新版本（可以复用原文件内容但使用新 storage key / version）。
- 归档立即从授权 snapshot 排除，无须等待 Qdrant 清除。已有在途 job 可以完成持久化但不得发布 current pointer；标 FAILED/DOCUMENT_ARCHIVED，之后清理临时索引。
- 历史 citation 展示版本标签；归档文档不向普通读者返回正文（即使旧会话仍留有引用），显示“来源已归档”。MANAGER 可审查归档内容。不把不可访问资料缓存到前端。

## Approval 状态机

`PENDING → APPROVED → EXECUTED | FAILED`；`PENDING → REJECTED | EXPIRED`；`APPROVED → EXPIRED`。PENDING 默认 10 分钟有效；APPROVED 也受相同 expires_at 约束。决策只接受 PENDING；过期校验发生在决策和执行，而非仅依赖清理任务。

批准与执行分两个 API：批准保存明确的人类决定；执行由 Java worker 或批准后前端调用触发；重启后扫描 APPROVED 可恢复。approved_by 必须等于 requester，且当前仍有业务权限。v1 没有跨人代批或双人复核；这里的 human-in-the-loop 是用户确认自己能做的事。

内容有误必须 REJECT 并新建草稿；不允许修改已展示/批准的 payload。FAILED 是终态，不把失败审批回转为 APPROVED；修正条件后新建草稿。数据库瞬时不可用导致事务完全回滚时仍保持 APPROVED，可按原 approval ID 重试。
