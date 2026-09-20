# Architecture Phase Report

项目：KnowledgeOps AI · 日期：2026-09-20 · 工作目录：`[本机工作目录]

本轮仅Architecture Phase。输出文档、接口/SQL/schema、评测数据、目录骨架、基础设施草案与架构检查工具；没有应用业务实现。

## 1. Product Scope
冻结为单企业内部知识库+工单+受控AI助手。员工/支持/知识管理员/管理员四角色，手动业务可独立完成。范围详见 [PRODUCT_SCOPE](docs/PRODUCT_SCOPE.md)。

## 2. Architecture
Vue→Java业务核心→Python AI服务。Java模块化单体，Python不拥有业务数据库或浏览器入口；服务间REST。文档用Java持久化job+租约处理。详见 [ARCHITECTURE](docs/ARCHITECTURE.md)。

## 3. Core Technology Choices
Java21/Spring Boot4.1.x/JPA/Flyway；Python3.12/FastAPI/Pydantic；MySQL8.4、Redis7.4、Qdrant单一向量库；Vue3/TypeScript/Vite；Compose/GitHub Actions。DeepSeek配置化，真实中文embedding选BGE small，Fake用于无key测试。[来源与版本边界](docs/REFERENCES.md)。

## 4. Domain
用户/部门/RBAC、KB/Document/Version/Chunk、Ticket/Comment/Assignment/StatusHistory、Conversation/Message/Citation、Approval/Execution/Audit。明确工单状态迁移、角色范围、版本发布与审批终态；Draft复用AIApproval，不另造表。[DOMAIN_MODEL](docs/DOMAIN_MODEL.md)。

## 5. Database
25张表，完整字段/PK/FK/Unique/Index/CHECK草案与41条FK关系ERD。核心事务、历史保留、soft archive和查询索引已说明；Qdrant只是派生索引。[DATABASE_DESIGN](docs/DATABASE_DESIGN.md)、[SQL](docs/database/schema.sql)、[ERD](docs/database/ERD.md)。

## 6. Java Backend
auth/user/knowledge/ticket/ai/audit/shared模块；每模块api/application/domain/infrastructure职责明确。事务统一用于手动业务和AI批准执行。当前只有源目录骨架，无Controller/CRUD/pom构建。

## 7. Python AI Service
api/core/providers/rag/agents/tools/schemas/services/evaluation/tests目录。parser、RAG、provider、tool client边界与DTO已定义。没有FastAPI路由实现、没有Python数据库依赖。

## 8. RAG
parse→normalize→chunk→embed→Qdrant prepare→Java manifest发布。查询以授权KB和current READY version/generation预过滤，Java提供经二次鉴权的正文，回答保留claims/citations。归档/撤权后历史回答也遮蔽。[AI_ARCHITECTURE](docs/AI_ARCHITECTURE.md)。

## 9. Agent
七个严格工具；最多3步/1个写草稿/30秒；Java驱动执行，Python只规划。工具JSON Schema禁止actor、department、admin身份字段。get/list/search只读，其他工具只产PENDING草稿。

## 10. Human Approval
不可变payload+hash+资源版本+过期时间；requester本人确认；执行时再鉴权，同一MySQL事务写业务/执行/审计/结果。approval_id唯一执行，失败/过期不能复用。审批不提升权限。

## 11. Audit
登录、上传/处理/归档、检索、工单行为、AI请求/决定/执行、管理变更事件目录已定义。actor/action/resource/metadata/timestamp/requestId齐全。关键写事务内append，敏感日志字段白名单。

## 12. Security
权限矩阵、IDOR、KB ACL、JWT+refresh轮换+session撤销、CSRF条件、注入、上传路径/ZIP限制、秘密与日志脱敏均有对应规则和负向测试计划。[SECURITY_MODEL](docs/SECURITY_MODEL.md)。

## 13. Docker
根Compose是可启动的MySQL/Redis/Qdrant依赖配置；application overlay定义frontend/java-backend/ai-service的未来真实镜像拓扑。只有前端发布端口，Python只读源文件、无MySQL网络和密码。当前未实现应用镜像；本轮环境Docker引擎未运行，不能声称已启动验证。[infra](infra/README.md)。

## 14. Testing
架构脚本与GitHub Actions校验文档/OpenAPI/严格工具/评测/ERD/Compose。Java/Python/Web完整测试层级和后续CI模板已设计。40条合成评测、20dev/20test、9个来源版本；尚无真实AI指标或业务测试成绩。[TEST_STRATEGY](docs/TEST_STRATEGY.md)、[evaluation](evaluation/README.md)。

## 15. Phase Plan
0架构→1Java foundation→2Ticket→3Knowledge pipeline→4RAG→5Agent+Approval→6Frontend→7Packaging，每阶段有可独立验证的出口。测试/基础设施从早期接入，不推迟到最终阶段。[IMPLEMENTATION_PLAN](docs/IMPLEMENTATION_PLAN.md)。

## 16. Major Tradeoffs
双语言增加契约维护，换取清楚的业务/AI边界；Qdrant增加一个容器，避免再引入第二套关系数据库；MySQL job换取简单恢复，吞吐先受控；显式version过滤适合小语料，1000 current文档为首版过滤预算；审批增加一次确认，换取可追溯意图与权限保障；完整启动验收留到真实镜像完成。[六项ADR](docs/ADR/README.md)。

## 17. What was intentionally NOT built
没有登录页、完整Vue、CRUD、真实RAG/Agent、Prompt调优或部署；没有Kubernetes/Service Mesh/Kafka/RabbitMQ/gRPC、多租户、billing、OCR、多Agent、复杂BPMN。架构工具脚本不属于业务实现。

## Final validation evidence

实际执行结果：

| 检查 | 结果 / 边界 |
| --- | --- |
| `.venv/Scripts/python.exe scripts/validate_architecture.py` | PASS：必需文档、6 ADR、26 Markdown文件链接集合 |
| OpenAPI 3.1标准validator | PASS：Java public 52 + Java internal 4 + Python 6，共62个operation |
| Tool JSON Schema | PASS：7个工具，合法样例+8个非法身份/未知工具样例 |
| 契约重新生成 | PASS：重新生成前后4份JSON的SHA-256完全一致 |
| Evaluation artifacts | PASS：40 case IDs/schema/工具参数/授权黄金证据锚点；20dev/20test，9个source |
| SQL / ERD结构覆盖 | PASS：25张表、41条FK关系，包含同document版本指针与approval唯一执行约束 |
| Compose安全布局 | PASS：Python无business-data网络/业务库配置，无公开端口，源文件只读 |
| `docker compose --env-file .env.example config --quiet` | PASS：基础设施配置解析 |
| 合并`infra/compose.application.yml`的Compose config | PASS：未来完整六服务配置解析 |

本机架构验证运行在Python3.13虚拟环境；应用目标与CI配置仍为Python3.12，本轮未做应用运行时兼容性验证。Docker daemon不在线；SQL真实MySQL执行、镜像拉取/启动、Java/Python/Web业务测试、真实Provider评测均未执行，属于已明确的后续阶段验收，不在本报告中伪称通过。

人工一致性复核：手动工单与AI执行共用权限/状态机；审批后再鉴权与版本校验；MySQL/Redis撤销失败恢复不会复活family；检索前过滤+正文返回鉴权+输出epoch检查；历史对话/幂等响应/审批证据依赖也受撤权约束；index prepare在发布前不可检索；所有关键写与audit同事务。未发现阻断Phase1的未决架构矛盾。

## Readiness

Ready表示可以按冻结契约进入Phase1，不表示产品已可运行；容器运行、DDL迁移和业务集成是后续各阶段明确的验收项。

```text
ARCHITECTURE_READY = true
PRODUCT_SCOPE_FROZEN = true
DATABASE_DESIGN_READY = true
API_CONTRACT_READY = true
AI_ARCHITECTURE_READY = true
IMPLEMENTATION_PLAN_READY = true
CODE_IMPLEMENTATION_STARTED = false
```
