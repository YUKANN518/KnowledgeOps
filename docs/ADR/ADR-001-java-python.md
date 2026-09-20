# ADR-001: Java + Python dual backend

Status: Accepted · 2026-09-20

## Context
需要同时展示企业事务/RBAC工程和Python AI能力，但单一业务事实来源必须清楚。

## Decision
Java21/Spring Boot拥有业务、用户、审批、审计和所有MySQL写入；Python3.12/FastAPI实现parser/RAG/provider/有限planner。浏览器仅调用Java，服务间受控REST。

## Alternatives
全Java减少部署单元但弱化Python生态与求职目标；全Python弱化Java企业后端展示；每领域一个微服务增加不必要协调成本。

## Consequences
需要维护两套契约、超时与错误映射；Python不持有MySQL凭据，部署网络隔离。AI不可用不影响手动工单。Java提供chunk manifest持久化，但不参与切块算法。
