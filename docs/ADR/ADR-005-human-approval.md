# ADR-005: Human approval before AI writes

Status: Accepted · 2026-09-20

## Context
模型会误解意图、产生非法参数或受检索内容注入。角色验证无法代替用户对具体写入内容的确认。

## Decision
所有AI业务写先存不可变PENDING草稿，绑定requester、payload hash、版本和期限；用户专用确认API批准，Java再鉴权和事务执行；唯一approval execution+业务事务幂等。read/draft/execute记录分开，AI服务无approve/execute权限。

## Alternatives
模型自认“低风险”直接执行难以约束；只在prompt里要求确认不能构成安全边界；所有行为双人审批过度复杂；手动CRUD再走审批妨碍基础流程。

## Consequences
多一个确认交互与状态恢复worker，换取可解释的意图/决定/效果审计。自己批准只确认本来有权的操作，不是四眼审批，也不会提权。过期/撤权/版本变更需要重新生成草稿。
