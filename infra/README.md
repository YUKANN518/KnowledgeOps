# Infrastructure architecture

> **Phase 0 design archive:** the implemented runtime is the root `docker-compose.yml` with frontend, Java, MySQL, Redis, and uploads storage. Later-phase AI topology below is not delivered.

`docker-compose.yml` 可以启动 MySQL/Redis/Qdrant。本轮没有应用镜像，所以完整三层业务启动不可执行；不能把占位镜像或HTTP空壳说成可演示产品。`compose.application.yml` 是完整六服务拓扑的 Compose 可解析草案，Phase7构建三个实际镜像后合并使用。当前版本标签是兼容性基线，Phase7核验镜像存在性并固定digest；本轮尚未拉取镜像。

```powershell
docker compose --env-file .env.example config --quiet
docker compose --env-file .env.example -f docker-compose.yml -f infra/compose.application.yml config --quiet
```

完整镜像需满足：frontend包含静态Vue构建和非root Nginx，8080只转发/api到Java；Java21 JRE镜像提供 `/app/healthcheck`（实现阶段添加）且监听8080/8081；Python3.12非root镜像准备 parser 依赖、模型cache和API。Java readiness只依赖MySQL/Redis，不因AI宕机阻断工单；Python readiness检查Qdrant/Java内部服务，Provider异常标degraded。Compose初始顺序不能替代运行时重连。

`service-internal` 是服务间专用Docker bridge。Java和Python互通；Python不在business-data网络，没有MySQL/Redis凭据；Java不在ai-data网络，没有Qdrant凭据。Provider访问由ai-egress出网。原文件volume由Java读写、Python只读，Python仍属于可读文档的可信处理服务；防越权保障针对用户/模型请求，不宣称能隔离已完全攻陷的AI容器。

不发布数据库/Python端口。Phase1本机IDE开发可通过单独开发override绑定 `127.0.0.1` 端口，不能沿用到公开部署。数据卷持久化，普通 down 不删卷；`down -v` 为显式破坏性重置操作，不在自动脚本执行。clone后复制.env设置口令，普通CI使用临时凭据。

Phase7配置用户UID/GID确保document volume共享读权限，root只用于volume初始化且不作为应用运行用户。限制上传大小、解析时间/内存，日志 stdout JSON；生产部署再加TLS终端，不引入Kubernetes。

备份：进入维护窗口暂停上传和审批写入→MySQL逻辑备份→复制原文件→恢复演练→Qdrant重新索引或快照。刷新会话不属于必要备份，Redis丢失需重新登录。禁止备份.env到公开路径。
