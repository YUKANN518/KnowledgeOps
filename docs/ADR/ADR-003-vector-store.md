# ADR-003: Qdrant as the single vector store

Status: Accepted · 2026-09-20

## Context
核心数据已经选MySQL，需要可在Compose内维护、支持metadata过滤的向量检索；不为技术数量部署多套库。

## Decision
Qdrant单节点；Python封装VectorStore接口，使用payload filter和keyword indexes；只存vector+IDs/版本元数据，正文与权限事实在Java/MySQL。Fake与BGE collection隔离，512维cosine，index可重建。

## Alternatives
pgvector本身合理，但MySQL已固定，再引入Postgres仅作向量存储需维护两套关系数据库；Milvus适合更大规模但此处运维复杂度无收益；内存索引难以验证持久化/过滤/恢复。

## Consequences
增加一个容器及跨存储发布协议。没有MySQL跨库事务；使用先准备generation再发布current指针解决半可见问题。显式过滤最多1000个current版本，超出时拒绝而非放宽ACL；规模提升再研究分区与授权索引。
