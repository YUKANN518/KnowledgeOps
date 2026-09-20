# Architecture decision records

状态均为 Accepted for v1 architecture；实现依次落地。变更时追加新 ADR，保留原决定上下文。

| ADR | 决定 |
| --- | --- |
| [001](ADR-001-java-python.md) | Java业务核心 + Python AI能力层 |
| [002](ADR-002-modular-monolith.md) | Java模块化单体 |
| [003](ADR-003-vector-store.md) | 只使用Qdrant向量库 |
| [004](ADR-004-rest.md) | REST + MySQL持久化处理任务 |
| [005](ADR-005-human-approval.md) | 所有AI业务写先人工确认 |
| [006](ADR-006-mysql.md) | MySQL核心数据，JPA+Flyway |
