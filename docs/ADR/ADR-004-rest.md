# ADR-004: REST communication and durable Java jobs

Status: Accepted · 2026-09-20

## Context
问答是交互请求，文件处理可能较慢，但首版吞吐低，需要失败恢复而非消息中间件演示。

## Decision
REST/OpenAPI，requestId传播、有限超时、明确幂等；上传202返回MySQL job，Java定时worker领取租约并同步调用Python处理。Python只返回manifest，Java事务发布。

## Alternatives
浏览器等待全文件处理体验差且易丢请求；FastAPI BackgroundTasks不是持久队列；Celery/Dramatiq+Redis可用但增加worker和一致性维护；Kafka/RabbitMQ/gRPC均无当前必要性。

## Consequences
Java worker保留长请求资源，首版并发1、文件/时长预算明确。重试靠lease token和generation隔离，不靠“恰好一次投递”。需要更高吞吐时测量后迁移任务执行层。
