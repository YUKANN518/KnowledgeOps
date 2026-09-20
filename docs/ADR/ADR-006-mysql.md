# ADR-006: MySQL core business data

Status: Accepted · 2026-09-20

## Context
工单、角色、审批、引用证据需要关系约束、本地事务和可演示的查询索引。

## Decision
MySQL8.4/InnoDB、Spring Data JPA、Flyway；Java唯一写入者。业务状态、审批执行和审计同事务。source文件独立私有volume，元数据hash入库；chunk正文入库以便Java鉴权后返回。

## Alternatives
Postgres可完成业务但偏离已给定MySQL目标；MongoDB使关系一致性收益变小；同时用JPA/MyBatis无已知复杂查询收益；让Python直写核心表破坏授权边界。

## Consequences
不能与Qdrant做单一事务，使用可重建索引与发布指针。TEXT/chunk增加MySQL体积，首版10k chunks预算足够，之后按实际体积评估对象存储。测试使用真实MySQL而非H2掩盖差异。
