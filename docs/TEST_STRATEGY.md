# Test strategy

## 当前和未来的测试边界

Phase 0 执行 `python scripts/validate_architecture.py` 检查 required files、内部 Markdown 链接、OpenAPI/schema、工具禁止身份字段、SQL 表/ERD覆盖、评测 case IDs/证据锚点/数量；`docker compose config --quiet` 解析部署配置。这些检查不证明业务逻辑已实现，也不替代运行 MySQL DDL 或镜像启动。

| 层 | 使用工具 / 必测行为 | 数据依赖 |
| --- | --- | --- |
| Java unit | JUnit：Ticket 全状态矩阵、权限谓词、审批状态与期限、规范化 hash | 固定 Clock/UUID；无容器 |
| Java service | 事务、版本冲突、手动和审批同用例、审计失败回滚 | Mockito 仅 mock 边界 |
| Repository | FK/unique/index、分页、锁、并发重复执行 | Testcontainers MySQL 8.4，不能用 H2 代替 |
| Controller | Spring Boot Test / MockMvc：schema/error/403/404、身份字段拒绝、cookie/CSRF | Fake AI client |
| Integration | MySQL+Redis、refresh 重放、并发 token rotate、job lease恢复、审批重启恢复 | Testcontainers + Fake Python HTTP |
| Python provider | 超时/429/schema错误、DeepSeek请求格式、embedding维度/归一化 | fixtures，禁止真实 key |
| Python RAG | parser顺序/页码、chunk边界、强制filter、manifest、引用子串检查/拒答 | 本地文件，Qdrant integration 独立标记 |
| Python tool/API | Pydantic extra=forbid、unknown tool、越权目标、context过期、tool-result去重 | Fake Java context/evidence API |
| Evaluation | retrieval/citation/groundedness/permission/tool 指标 | 40 个 fixtures，真实模型 opt-in |
| Frontend | Vitest components：错误、引用、审批 payload/hash/过期、状态按钮、禁用重复提交 | MSW 契约 mock |
| End to end later | Playwright：登录→查知识→草稿→确认→实际工单，外加纯手动工单 | Phase 7 全 Compose+Fake Provider |

## 必须覆盖的故障场景

1. 两个请求同时批准或执行同一草稿：结果一致，只一个 ticket / assignment / history / audit mutation。
2. 批准后撤角色、调部门、停用用户或 ticket.version 增加：不得写入，FAILED 原因可查。
3. APPROVED 后 Java 重启：恢复 worker 最多执行一次；事务 commit 后响应丢失再次调用返回原结果。
4. 上传后 Java/Python 分别在领取、向量写入、manifest 返回、发布前后崩溃：无半可见版本，lease恢复不覆盖新 generation。
5. Qdrant 有其他部门和孤儿 vectors：查询过滤不得产生相关 hit，evidence endpoint二次拒绝；空 ACL 不可全局检索。
6. 检索后、prompt构造前及返回前撤 ACL：epoch不符丢弃；会话旧回答不能绕过撤权。
7. AI payload 注入 requesterId/departmentId/adminRole、审批后修改 payload/hash：422/409，无业务效果。
8. 新版本发布后旧引用可定位旧文本；归档后普通用户历史回答被隐藏。
9. Provider/vector服务不可用时，手动创建和处理工单继续成功；Redis挂掉认证拒绝放行并给明确503。
10. XML/ZIP bomb、PDF扫描件/超页数、路径穿越、HTML脚本、SQL sort注入都被边界拒绝。
11. 登出数据库提交后Redis撤销失败，Redis恢复仍不可复活family；幂等问答在撤权后重放不能取回旧答案；草稿引用证据失权后不能批准/执行。

## CI

当前 `.github/workflows/architecture.yml` 仅检查本轮实际存在的交付。`infra/ci/application-ci.yml.example` 是后续 job 模板，不放到 workflows 中假装测试通过。Phase 1/3/6 分别启用模块 job，路径过滤可减少开销，但不能跳过公共契约变化触发的测试。

- Java：Temurin21，Maven wrapper verify（含 unit/repository/controller）；缓存 Maven。
- Python：Python3.12，锁依赖安装，ruff check、mypy app、pytest；评测 Fake suites；集成 Qdrant 独立 job。
- Web：Node LTS（Phase6锁定）/npm ci、type-check、lint、test -- --run、build。
- contracts：所有 PR 都运行；真实 LLM evaluations 手动 workflow_dispatch 或本地，secrets 不输出日志，不用于未信任 fork PR。
- E2E 在 Phase7 引入，默认 PR 不跑重量级全链路；nightly/手动执行小套件。

不以覆盖率数字代替状态机和权限组合测试；关键规则应有至少一个正例、一个负例、一个并发/失效例。交付报告必须分别列静态检查、容器集成、真实 Provider 与人工评审的实际完成情况。
