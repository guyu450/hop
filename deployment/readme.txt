
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
恢复3: 恢复feature中对多余plugins的删除 17个
    hop-transform-systemdata
    hop-transform-tablecompare
    hop-transform-tableexists
    hop-transform-tableinput
    hop-transform-tableoutput
    hop-transform-terafast
    hop-transform-tika
    hop-transform-tokenreplacement
    hop-transform-update
    hop-transform-valuemapper
    hop-transform-verticabulkloader
    hop-transform-webserviceavailable
    hop-transform-webservices
    hop-transform-workflowexecutor
    hop-transform-writetolog
    hop-transform-yamlinput
    hop-transform-zipfile