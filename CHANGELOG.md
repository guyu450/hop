# Changelog
本地化修改会记录在案
## [2.16.2] - 2026-02-27
### Added
- 
### Fixed
- 修复2.14.1 升级到2.16.1 时候的plugins的合并问题

## [2.16.1] - 
### Changed
- 升级2.14.0 到2.16.0
### Added
- 增加UI界面中metadata的列表控制方法：config 目录下的metadata-filter-config.json 配置 excluded 和 included；对应实现类：ConfigurableMetadataFilter；
  jar包依旧会加载；
  {
  "included": [
  "rdbms",
  "MinioConnectionDefinition"
  ],
  "excluded":[]
  }
  下方为完整key
  "async-web-service",
  "AzureConnectionDefinition",
  "cassandra-connection",
  "dataset",
  "rdbms",
  "execution-data-profile",
  "execution-info-location",
  "GoogleStorageConnectionDefinition",
  "neo4j-graph-model",
  "server",
  "MailServerConnection",
  "MinioConnectionDefinition",
  "mongodb-connection",
  "neo4j-connection",
  "partition",
  "pipeline-log",
  "pipeline-probe",
  "pipeline-run-configuration",
  "unit-test",
  "restconnection",
  "salesforceconnection",
  "schema-definition",
  "splunk",
  "variable-resolver",
  "web-service",
  "WorkflowLog.name",
  "workflow-run-configuration",
  "WorkflowLog.name"


## [2.14.1] -
### Initial Release
- 首次使用2.14.0 版本
### Changeed
- 提示汉化
- 帮助文档汉化翻译
- 许可证生成和验证