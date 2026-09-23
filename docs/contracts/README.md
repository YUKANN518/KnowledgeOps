# Machine-readable contracts

> **Phase 0 design archive:** these generated contracts include proposed endpoints that are not all implemented. Use the running backend OpenAPI document for delivered behavior.

- `java-public.openapi.json`：浏览器→Java。
- `java-internal.openapi.json`：Python→Java，服务身份与opaque context。
- `ai-service.openapi.json`：Java→Python。
- `tool-schemas.json`：七工具独立 JSON Schema Draft 2020-12，可用于Pydantic/Java入参验证与模型tool definitions。

生成来源 `scripts/build_contracts.py`；修改后执行 `python scripts/build_contracts.py` 和 `python scripts/validate_architecture.py`。OpenAPI 可导入 Swagger Editor 或 API 客户端进行开发；当前无服务器/mock业务实现。通用错误响应在契约中列出，不代表每个路由必定产生所有错误。

字段不是越权入口：在管理员 DTO 中 departmentId/userId 是合法管理目标；这些字段不能出现在模型的工具身份上下文。请求schema严格，输出的可选/nullable字段有明确类型。条件业务规则（状态迁移必须备注、部门一致、版本合法）在 Domain/Security 文档定义，实施阶段必须落在应用服务，不能只靠schema。
