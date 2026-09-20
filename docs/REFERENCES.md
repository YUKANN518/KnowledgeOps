# Technical references

架构核对日期：2026-09-20。只用于接口能力与兼容边界，不宣称依赖永远为最新。

- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)：查阅时 4.1.1，Java21 在兼容范围；实施固定 Maven BOM 补丁版本。
- [Qdrant filtering](https://qdrant.tech/documentation/search/filtering/)：向量请求支持 payload filters；授权过滤必须由本服务构造。
- [Qdrant payload indexes](https://qdrant.tech/documentation/manage-data/indexing/)：为过滤字段建立索引；不能把 payload ACL 当业务授权真相。
- [BGE small Chinese model card](https://huggingface.co/BAAI/bge-small-zh-v1.5)：中文 embedding 基线与 revision 记录来源。512维/512输入上限在实施 contract tests 再核验。
- [DeepSeek model API](https://api-docs.deepseek.com/api/list-models/)：模型 ID 从配置与账户能力确认；本设计不绑定示例中的动态 model 名。

选 Qdrant、本地 BGE 和 REST 是本项目对可维护性的判断，不是来源对本项目的性能背书。没有进行向量库横向 benchmark。
