# 开发备忘录

## 地图 Key 管理

当前需求广场已支持可选接入高德 JavaScript API。MVP 阶段为了保持本地可运行，页面允许在浏览器中填写 Web Key 和安全密钥，并保存到 `localStorage`。

后续生产化时需要调整：

- 高德 Web Key、`securityJsCode` 不得提交到仓库。
- dev/test/prod 环境分别配置独立 Key，避免演示环境影响正式配额。
- 推荐由后端配置或环境变量下发前端运行时配置，例如 `/api/config/map`。
- 生产环境应限制 Key 的来源域名，并定期轮换。
- README 中只保留配置方式和占位示例，不出现真实 Key。

## XLSX 导入

`example.xlsx` 已作为当前需求导入格式的样例文件。后续迁移 Spring Boot 后，建议把本地路径导入替换为 multipart 上传，并在服务端保存导入批次、失败行和原始文件归档记录。
