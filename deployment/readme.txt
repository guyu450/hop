feature/2.16.2:
    2.16.1 : 将2.14.1 合并到2.16.1 时候发生了【去除多余plugins】的pom文件 assemblies/plugins/pom.xml 合并错误；
    新增2.16.2: 启始于2.16.0 ， 将2.14.1 和2.16.1 的修改合并到2.16.2

feature/2.16.1:
修改1:升级2.14.0 到2.16.0
修改2: 增加UI界面中metadata的列表控制方法：
        1. config 目录下的metadata-filter-config.json 配置 excluded 和 included；对应实现类：ConfigurableMetadataFilter；
           jar包依旧会加载；
           {
             "included": [
               "rdbms",
               "MinioConnectionDefinition"
             ],
             "excluded":[]
           }
        2. 修改文件。。。， 直接在打镜像的时候，将jar包不复制进去；等待中。。
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



feature/2.14.1
功能1: 汉化
功能2: 去除多余plugins
功能3: 帮助文档汉化翻译
功能3: 许可证生成和验证