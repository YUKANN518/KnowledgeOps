# Python AI 服务骨架

> **未实现：** 此空骨架保留自 Phase 0 设计阶段。Python、AI、RAG 和向量检索不属于已完成项目范围。

Phase3启用Python3.12、FastAPI、Pydantic、httpx、parser与Qdrant client；依赖由pyproject与lock固定。无SQLAlchemy：首版没有Python自有数据库。`app/`按架构分包，目录当前不含实现。

默认Fake Provider，不读取真实key；真实embedding/LLM在Phase4单独启用。api只暴露内网，所有请求严格schema，parser与tool client禁止任意网络URL和用户路径。
