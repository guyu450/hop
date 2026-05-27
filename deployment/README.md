# 1. 配置文件介绍：
- hop-data/config/
  - metadata-filter-config.json UI界面中metadata的动态列表(非必选项)
- hop-data/pwd/
  - hop.pwd 密码文件
- hop-data/lic/
  - public.pem 公钥
  - license.pem 注册码
  - machine_code 机器码
# 2. 获取注册码
- 找对接人要注册码、和公钥
# 3. 密码设置
## 3.1密码生成
- SHA-256+base64格式（推荐 - Web UI）
    ``` bash
    ./deployment/encrypt-password.sh test
    SHA256:n4bQgYhMfWWaL+qgxVrQFaO/TxsrC4Is0V1sFbDwCgg=
    ```
- SHA-256+Hex（同时支持 Web UI 和 REST API）
  ``` bash
  ./deployment/encrypt-password.sh test hex
  SHA-256:9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08
  ```
- 明文（不推荐 - 同时支持 Web UI 和 REST API）

## 3.2 密码配置
    位置：/hop-data/pwd/hop.pwd
    SHA-256+base64格式（推荐，一般仅webui使用）：
        htfx:SHA256:EaHBb37OkB6f6AnFQDBMG89KXOAokjkA6IjPE05zMAU=,default
    SHA-256+Hex格式：
        hftx:SHA-256:11a1c16f7ece901e9fe809c540304c1bcf4a5ce028923900e888cf134e733005,default
    明文格式：
        hftx:htfx@2016!,default

# 4. 启动
## 4.1 需要动态调整UI界面中metadata的列表的情况 
- 初始化配置文件目录：默认为 ./hop-data/config/ 和./hop-data/audit/
    ``` bash
    chmod +x init-hop-data.sh
    ./init-hop-data.sh
     ```
- 修改配置文件config/metadata-filter-config.json 调整内容
    ``` json
    {
    "included": [
    "rdbms",
    "MinioConnectionDefinition"
    ],
    "excluded":[]
    }
     ```
    
    key值如下：
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

- 启动容器
``` bash
    docker-compose -f docker-compose-dynamic.yml up -d
```

## 4.2 不用调整动态UI界面中metadata的列表情况
``` bash
    docker-compose -f docker-compose.yml up -d
```
## 5.访问地址

| 功能 | URL | 说明 |
|------|-----|------|
| **Web UI** | http://localhost:9003/ui | 图形化界面（需登录） |

## 登出
- Web界面登出：刷新页面重新登录
- Hop Server登出：访问 http://localhost:9003/logout