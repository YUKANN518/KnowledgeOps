# KnowledgeOps 3–5 分钟演示脚本

## 准备

1. 执行 `docker compose --env-file .env up -d --build` 启动完整环境。
2. 在仓库根目录运行 `.\frontend\tests\runtime-smoke.ps1`，保存输出中的普通员工、支持人员和知识管理员邮箱。
3. 打开 <http://localhost:5173>，密码使用 smoke 脚本中定义的本地演示密码。
4. 在编辑器中提前打开 `docker compose ps`、Flyway migration 和集成测试目录。

smoke 脚本创建的账号和内容都是可丢弃的本地演示数据。

## 0:00–0:35 — 登录与工作台

使用脚本生成的普通员工账号登录。

- 展示界面中的当前角色，以及后端返回并映射为中文的权限。
- 刷新页面，演示通过 HttpOnly Refresh Cookie 恢复会话。
- 说明 Access Token 只保存在内存中，并发 401 会共用一次刷新请求。

## 0:35–1:35 — 工单流程

- 打开**工单**，创建一个简短的办公问题。
- 进入详情页并添加评论。
- 退出登录，再使用支持人员账号登录。
- 打开同一工单，使用 smoke 输出中的支持人员 UUID 完成分配，并将状态改为**处理中**。

说明分配、状态流转、乐观锁版本检查和审计写入都在后端完成。当前状态机不支持的流转会返回 HTTP 409。

## 1:35–2:05 — 资源级授权

使用普通员工账号访问另一个员工创建的工单；若演示时间有限，也可以展示现有 smoke 结果。

- 即使调用者知道工单 UUID，后端仍会返回 403。
- 前端权限检查让导航更清楚，Spring Security 与资源级检查才是最终安全边界。

这是项目中最直观的 IDOR 防护示例。

## 2:05–3:10 — 知识文章与文档

使用知识管理员账号登录。

- 创建文章草稿，编辑后发布。
- 上传一个小型 TXT 或 PDF 文档。
- 展示文档列表和归档操作。

再切换为普通员工账号：

- 阅读已发布文章。
- 下载有效文档。
- 展示普通员工无法看到草稿、已归档内容和管理按钮。

说明 20 MiB 限制、扩展名/MIME/文件签名校验、UUID 存储名、私有 volume 和后端下载鉴权。

## 3:10–4:15 — 工程证据

简要展示以下目录：

- `backend-java/src/main/resources/db/migration/`：Flyway `V1`–`V3`。
- `backend-java/src/test/java/com/knowledgeops/`：Testcontainers 集成测试。
- `docker-compose.yml`：前端、后端、MySQL、Redis 和 uploads volume。
- `frontend/tests/runtime-smoke.ps1`：可重复执行的全栈 smoke 流程。
- `PHASE_5_PORTFOLIO_FINISH_REPORT.md`：最终验收证据与已知限制。

说明 MySQL 是业务数据的事实来源，Redis 校验活动会话；审计元数据有意排除凭据、Token 和正文。

## 4:15–5:00 — 收尾

将项目概括为一个范围克制的内部工具，重点展示 Java 后端基本功：身份认证、权限控制、事务、并发、migration、安全文件 I/O、集成测试和容器化运行。

演示结束后执行 `docker compose --env-file .env down` 停止环境。
