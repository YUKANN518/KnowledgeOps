# AI architecture

## 文档处理与 parser contract

`DocumentParser.supports(mediaType) -> bool`；`parse(source: BinaryIO, limits: ParseLimits) -> ParsedDocument`。输出 `blocks[{text,pageStart,pageEnd,headingPath,ordinal}]`、warnings、parserVersion；不返回模型生成文本。实现 PdfParser（文本 PDF）、DocxParser（段落/标题/表格按行提取）、TextParser（UTF-8 TXT/Markdown）。扫描 PDF 无可提取文字时 OCR_REQUIRED；加密 PDF 返回 DOCUMENT_ENCRYPTED；不静默生成空 READY。

Normalizer 统一换行和 Unicode NFC，保留代码块、标题和列表语义，记录 normalized text offsets；PDF offset 是归一化文本偏移，不假装对应原 PDF 字节。Chunker 按标题/段落递归切分，起点 320 tokens、overlap 48、硬上限 440；采用 embedding 模型 tokenizer，给 512 上限模型留标题/前缀空间；代码块/表格超限按行切分并保留 heading。以上为初始参数，Phase 4 使用 dev 集调整且记录版本。

每 chunk：UUID、knowledgeBaseId、documentId、versionId、generation、ordinal、text、textSha256、tokenCount、pageStart/pageEnd 可空、headingPath、charStart/charEnd、parserVersion/chunkerVersion/embeddingRevision。v1 每文件最多 2000 chunks，manifest 上限 8MiB，超限失败并提示拆分文件。同次 attempt chunk ID 由 version+generation+ordinal 确定性生成。

## Provider abstraction

| 接口 | 入参 | 输出 / 错误 |
| --- | --- | --- |
| LLMProvider.generate | messages、结构化 response schema、tools allowlist、deadline | text/claims/citations 或 tool proposals、usage、modelRevision；timeout/rateLimit/unavailable |
| EmbeddingProvider.embedDocuments | texts、modelRevision | normalized float vectors + dimension |
| EmbeddingProvider.embedQuery | text、modelRevision | 同 dimension/query instruction 的向量 |
| VectorStore.search | vector、强制 AuthorizedFilter、topK | chunk IDs + score，不含正文 |

DeepSeekLLMProvider 使用 OpenAI-compatible chat API，base URL/model/timeout 环境配置；兼容性通过 provider contract tests 确认，不把 model 名字写进业务逻辑。FakeLLMProvider 回放固定结构化 fixtures，可注入超时/非法引用/工具错误。FakeEmbeddingProvider 使用固定 hash/token 特征映射，dimension=512，只验证管线、无语义质量主张。

真实 embedding 独立选 `BAAI/bge-small-zh-v1.5`，512 dimensions，CPU 优先；运行时下载权重需显式准备，Phase 4 锁定 revision。DeepSeek 的 chat 接口不能被当作 embedding 接口。Fake 与真实 collection 分离 `knowledge_fake_v1` / `knowledge_bge_zh_v1`，启动校验 dimension 与 revision，不混用。语料或模型升级创建新 generation 并全量重新索引。

## Query pipeline

1. Java 验证用户/会话，保存 user message，生成 context；Python 开始处理。
2. Python 从 Java 获得新鲜授权的 KB + 当前 READY version/generation pairs；空权限/无 READY 文档直接 INSUFFICIENT_EVIDENCE。
3. Query embedding → Qdrant **带授权过滤的** top-8 search → 去重，按分数与 token budget 取最多 5 条；先滤权限再近邻检索，禁止全库检索后再删结果。
4. 通过 Java `/internal/ai/evidence` 批量获取最多 8 条正文，重新鉴权/epoch 校验。正文预算 2500 tokens，保留 ID、页码、标题和原文。
5. 构造“指令 / 用户问题 / 不可信 evidence”分隔的 prompt；只允许根据给定 evidence 回答，缺少信息则澄清或拒答。检索资料里的命令不构成系统指令。
6. LLM 返回 `answer, claims[{text,citationIds}], citations[{chunkId,quote}]`。CitationValidator 校验 ID 属于本次证据、quote 是 normalized text 子串、claim 至少有引用；补全元数据由服务完成，不采用模型自报页码/URL。
7. 引用结构错误最多修复一次且受剩余 deadline 约束，否则 INVALID_CITATION 拒答。引用存在不等于事实必然被支持；语义 groundedness 通过评测人工判定。
8. Java 再核验 epoch 和 evidence、保存 answer+citations+usage+audit 后返回。没有证据则返回明确不足及手动建单入口，不编造解决方法。

无 reranker 的 dense baseline；后续仅在 dev 集失败分析证明有收益时添加 reranker。v1 不上混合搜索/搜索引擎。score 阈值不能跨模型硬编码“0.8=可靠”；用 dev 集校准，test 集只报告。

引用响应字段：citationId、chunkId、documentId、versionId、title、pageStart/pageEnd、headingPath、quote、sourceUrl（Java 相对鉴权地址）。会话读取时再次校验，老版本能注明“非当前版本”；来源被撤权/归档则隐藏含其信息的回答，不只隐藏引用标签。

## 一个有限 Business Assistant

仅七个工具：searchKnowledge、getTicket、listTickets、createTicketDraft、assignTicketDraft、updateTicketStatusDraft、addTicketCommentDraft。严格参数见 [tool-schemas.json](contracts/tool-schemas.json)。不存在 executeTicket/statusWrite/delete/admin 工具。

Java 驱动循环：`/agent/plan` 提议一个工具或最终回答 → Java 验证 allowed tool/schema/current permission → 读取工具由 Java 执行，searchKnowledge 委托已鉴权的 RAG → 写工具调用 Java 草稿应用服务 → `/agent/tool-result` 将已清洗结果回填。每轮最多 3 次工具、1 个写草稿、30s、4000 output tokens；达到任意上限即结束提示用户。Python 无后台自主循环，无定时自主行动。

plan/result 使用 conversationId、turnId、toolCallId；Java 持久化 tool execution，校验 call/result 归属与去重。Python planner 无持久内存，Java传回经过重新鉴权的会话片段和有效 tool history。对模型仅提供工具数据（脱敏工单/草稿 ID），不提供内部 context/token。只返回草稿时回答必须“等待确认”，不能说已创建/已分配。

批准后 Java 执行的结果由浏览器读取审批详情；如果需要 AI 解释结果，开启新 turn 并重新鉴权，过期 context 不能恢复执行权限。工具与审批分离：一个工具请求可以只是读取/提出草稿，批准是人的决定，真正业务执行是另一个有唯一约束的记录，才能明确追踪谁提议、谁确认、是否成功。

## Failure and quality

Provider 失败返回 AI_PROVIDER_UNAVAILABLE 503，vector 失败返回 VECTOR_STORE_UNAVAILABLE 503；均保留手动业务入口。解析失败写 version/job 的 errorCode 与脱敏摘要，不把堆栈传前端。AI query 审计仅保存 knowledgeBase IDs、hit count、duration/token usage 和结果状态，不保存完整 prompt。

评测用例见 [evaluation](../evaluation/README.md)：真实 RAG 质量与 Fake 管线测试分开报告。权限泄漏/未审批写入是 release blocker，单个命中率或“引用存在率”不能代替安全验收。
