# Implementation phases

每一阶段合并前同步 API/SQL/ADR，所有阶段可独立演示与验证。下列命令是阶段建好工具链后的验收约定，不声称当前可执行；当前可执行命令单列在 README。

| Phase | 交付 | 独立验收 / 出口条件 |
| --- | --- | --- |
| 0 Architecture | 文档、SQL/ERD、OpenAPI、schema、40-case dataset、Compose 依赖与未来 overlay | 架构检查脚本、OpenAPI validator、Compose config 通过；核对权限/审批/版本一致性 |
| 1 Java foundation | Java21 Maven wrapper、Boot、模块骨架、Flyway、用户/部门/固定角色、JWT/refresh/session、审计、错误、CI Java job | `./mvnw verify`；真实 MySQL+Redis 容器登录/刷新/重放/登出/停用/403/404；无 AI key |
| 2 Ticket | 手动 create/list/detail/comment/assign/transition 与历史、乐观锁、幂等 | API runner 完成 OPEN→IN_PROGRESS→WAITING_USER→RESOLVED→CLOSED；非法迁移、跨部门访问、重复提交验证 |
| 3 Knowledge pipeline | KB 成员授权、私有上传、不可变版本、durable job、Python parser/chunker/fake embedding、Qdrant | PDF/DOCX/TXT/MD fixtures 可 READY；损坏/路径逃逸拒绝；重启 worker 可恢复；旧版本仍可引用 |
| 4 RAG | real embedding + DeepSeek adapter、Fake fixtures、query/search/citations、40-case evaluator | 无 key 模式契约全通过；真实模式单独 opt-in；retrieval/evidence/ACL 撤销验证；记录首份带配置和日期的评测结果 |
| 5 Agent + approval | 七工具、schema 校验、不可变草稿、确认/拒绝/过期、执行恢复、审计 | 并发 execute 只产生一个效果；伪造 actor、改 payload、撤权、过期、过时 version 全部拒绝 |
| 6 Frontend | 登录、KB 列表/上传/状态、问答+引用、工单、审批详情、基本管理 | type-check/lint/Vitest/build；API错误与degraded状态可操作；不依赖 AI 可走完整工单 |
| 7 Packaging | 全服务 Compose 镜像、锁依赖/镜像、CI 全矩阵、少量 Playwright、恢复演练、真实截图/演示脚本 | 新目录 clone 按 README 启动；健康检查、无 key 演示；测 p50/p95、重建索引、文档/评测最终校验 |

Phase 1 起即启动 Java CI，Phase 3 启动 Python CI，Phase 6 启动前端 CI，Docker 依赖从 Phase 0 使用；不要把测试延迟到最后。真实 Provider key 永远不作为普通 CI 前提。

## 关键依赖与演示主线

Phase 3 依赖身份/ACL；Phase 4 依赖可追溯 chunk manifest；Phase 5 依赖 Phase 2 的稳定应用服务，不重写一套“AI专用工单业务”。前端可使用契约 mock，但 mock 页面不能被计入后端验收。

最终 8 分钟面试演示：Employee 检索 VPN 指南并打开版本引用 → 无证据问题拒答 → AI 生成工单草稿 → 人工核对并确认 → Support 分配/处理/解决 → Employee 关闭 → Administrator 查 requestId 审计链 → 演示撤权检索为空、重复执行只产生一张单。

风险与止损：embedding 权重下载/CPU 太慢时仍提供 Fake 管线演示并注明；权限过滤或审批原子性没有通过负向测试则不进入对外真实资料演示；稀疏语料检索差先修语料/chunk/评测，不引入多 Agent。
