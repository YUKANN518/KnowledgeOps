# ADR-002: Modular monolith

Status: Accepted · 2026-09-20

## Context
一个人维护的作品集需要事务清楚、模块可解释，不应形成巨型Service或微服务集群。

## Decision
auth/user/knowledge/ticket/ai/audit/shared七个包模块，api/application/domain/infrastructure按职责组织。模块经应用API调用；Spring事务串联工单与审批审计。用ArchUnit检查跨模块repository访问（Phase1）。

## Alternatives
传统全局controller/service/repository易变成横向巨包；完整DDD多重抽象增加入门成本；独立微服务使本地事务变为分布式问题。

## Consequences
部署仍为一个Java进程；通过包边界和测试管理依赖，不宣称数据库级模块隔离。只有规模/团队需求证明必要时再拆。
