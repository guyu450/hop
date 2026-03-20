# Apache Hop 项目技术栈与架构分析

## 一、项目概述

Apache Hop 是一个开源的数据编排平台（版本 2.16.0），由 Apache Software Foundation 维护，采用 Apache 2.0 许可证。项目于 2020 年创立，是对 Kettle/Pentaho Data Integration 的演进版本。

### 核心定位
- **ETL 工具**: 支持数据提取、转换和加载
- **数据编排平台**: 提供可视化的数据管道和工作流设计
- **批处理和实时处理**: 支持离线批处理和实时流式处理
- **可扩展架构**: 基于插件体系，支持自定义扩展

---

## 二、技术栈

### 2.1 编程语言与运行环境

| 技术 | 版本 | 说明 |
|------|-------|------|
| Java | 17 | 主要开发语言 |
| Maven | 3.6.3+ | 项目构建和依赖管理 |
| JUnit | 5.10.2 | 单元测试框架 |
| Mockito | 5.12.0 | Mock 测试框架 |

### 2.2 核心依赖库

| 类别 | 技术组件 | 版本 | 用途 |
|------|-----------|------|------|
| **序列化** | Jackson | 2.15.4 | JSON/XML/Avro 序列化 |
| **日志** | Log4j | 2.23.1 | 日志管理 |
| **CLI** | PicoCLI | - | 命令行界面 |
| **验证** | Commons Validator | 1.10.0 | 数据验证 |
| **压缩** | Snappy | 1.1.10.7 | 压缩算法 |
| **XML** | Apache Batik | 1.17 | SVG 和 XML 处理 |
| **图形** | Eclipse SWT | 3.131.0 | 本地 GUI 界面 |
| **JPA 注解处理** | Jandex | 3.2.2 | 类索引生成 |
| **代码质量** | Spotless | 2.43.0 | 代码格式化（Google Java Format） |
| **代码质量** | Checkstyle | - | 代码规范检查 |
| **测试覆盖** | JaCoCo | 0.8.12 | 代码覆盖率统计 |

### 2.3 Web 与 REST API

| 组件 | 版本 | 说明 |
|------|-------|------|
| Jersey | 3.1.11 | JAX-RS 实现，REST API 框架 |
| Jakarta Servlet API | 6.1.0 | Servlet 规范 |
| Jakarta XML Bind | 4.0.4 | XML 绑定 API |
| Jakarta SOAP API | 3.0.2 | SOAP 服务支持 |
| Jakarta Web Services | 4.0.2 | Web 服务规范 |

### 2.4 大数据支持

| 组件 | 版本 | 说明 |
|------|-------|------|
| Apache Beam | 2.62.0 | 统一批处理/流处理框架 |
| Hadoop | 3.4.1 | 大数据分布式存储和计算 |

### 2.5 数据库支持

项目通过插件支持 50+ 种数据库，包括但不限于：
- Oracle、MySQL、PostgreSQL、SQL Server
- MongoDB、Cassandra、Redis
- Hive、Spark、Impala
- ClickHouse、DuckDB、Snowflake
- 各种云数据库和时序数据库

---

## 三、项目架构

### 3.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                  用户界面层 (UI)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │  RCP UI   │  │  RAP UI   │  │  Web UI   │        │
│  └──────────┘  └──────────┘  └──────────┘        │
├─────────────────────────────────────────────────────────────┤
│                  引擎层 (Engine)                     │
│  ┌────────────┐         ┌────────────┐              │
│  │ Pipeline   │         │ Workflow   │              │
│  │  Engine    │         │  Engine   │              │
│  └────────────┘         └────────────┘              │
├─────────────────────────────────────────────────────────────┤
│                  核心层 (Core)                       │
│  ┌────────────┐  ┌────────────┐  ┌──────────┐  │
│  │ Plugin     │  │ Metadata   │  │ Config   │  │
│  │ System     │  │ Manager    │  │ Manager  │  │
│  └────────────┘  └────────────┘  └──────────┘  │
├─────────────────────────────────────────────────────────────┤
│                  插件层 (Plugins)                    │
│  ┌──────────────┬──────────────┬──────────────┐   │
│  │  Transforms  │   Actions    │  Databases   │   │
│  │  (150+)      │   (50+)      │  (50+)       │   │
│  └──────────────┴──────────────┴──────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 模块结构

#### 核心模块

| 模块 | 路径 | 功能描述 |
|--------|--------|----------|
| `core` | `/core` | 核心类库、插件系统、配置管理、元数据定义 |
| `engine` | `/engine` | Pipeline 和 Workflow 执行引擎 |
| `engine-beam` | `/engine-beam` | Apache Beam 扩展引擎 |
| `lib` | `/lib` | 通用库依赖集合 |
| `lib-jdbc` | `/lib-jdbc` | JDBC 驱动管理 |

#### 界面模块

| 模块 | 路径 | 技术栈 | 功能描述 |
|--------|--------|----------|----------|
| `ui` | `/ui` | Eclipse SWT | 桌面 GUI 客户端 |
| `rap` | `/rap` | Eclipse RAP | Web 界面（基于 RWT） |
| `rcp` | `/rcp` | Eclipse RCP | Rich Client Platform |
| `web` | `/assemblies/web` | HTML/JavaScript | 现代化 Web 界面 |
| `rest` | `/rest` | Jersey + Servlet | REST API 服务 |

#### 插件模块

| 模块 | 路径 | 功能描述 |
|--------|--------|----------|
| `plugins/actions` | `/plugins/actions` | 工作流动作插件（50+） |
| `plugins/transforms` | `/plugins/transforms` | 数据转换插件（150+） |
| `plugins/databases` | `/plugins/databases` | 数据库连接插件（50+） |
| `plugins/engines` | `/plugins/engines` | 执行引擎插件 |
| `plugins/misc` | `/plugins/misc` | 杂项工具插件 |
| `plugins/tech` | `/plugins/tech` | 技术相关插件 |
| `plugins/resolvers` | `/plugins/resolvers` | 资源解析器 |
| `plugins/valuetypes` | `/plugins/valuetypes` | 自定义值类型 |
| `plugins/customer` | `/plugins/customer` | 自定义客户插件 |

#### 打包模块

| 模块 | 路径 | 功能描述 |
|--------|--------|----------|
| `assemblies/client` | `/assemblies/client` | 桌面客户端打包 |
| `assemblies/web` | `/assemblies/web` | Web 应用打包 |
| `assemblies/static` | `/assemblies/static` | 静态资源打包 |
| `assemblies/plugins` | `/assemblies/plugins` | 插件独立打包 |

---

## 四、核心功能模块

### 4.1 Pipeline（数据管道）

Pipeline 是 Hop 的核心数据处理概念，通过可视化连接各种 Transform 组件来处理数据流。

**核心特性：**
- 可视化设计器，拖拽式构建
- 支持并行和分布式执行
- 实时数据预览
- 支持数据分区和负载均衡
- 错误处理和重试机制

**主要 Transform 类别：**
1. **输入类** - 文件、数据库、API、消息队列等
2. **输出类** - 文件、数据库、API、消息队列等
3. **转换类** - 数据清洗、格式化、计算、聚合等
4. **查找类** - 数据库查找、流查找、缓存查找等
5. **连接类** - 合并、联合、排序、分组等
6. **脚本类** - JavaScript、Python、SQL 脚本执行
7. **大数据类** - Hadoop、Spark、Beam 相关

### 4.2 Workflow（工作流）

Workflow 用于协调和管理 Pipeline 的执行顺序，支持复杂的业务流程编排。

**核心特性：**
- 条件分支和循环
- 并行执行控制
- 定时任务调度
- 事件触发机制
- 错误处理和邮件通知

**主要 Action 类别：**
1. **文件操作** - 复制、删除、重命名文件
2. **数据库操作** - 执行 SQL、表操作
3. **流程控制** - 判断、循环、等待
4. **任务执行** - 执行 Pipeline、子 Workflow
5. **网络操作** - HTTP、FTP、SSH、邮件
6. **系统操作** - Shell、脚本、发送消息

### 4.3 元数据管理

元数据（Metadata）是 Hop 的数据资产管理核心，包括：

**元数据类型：**
- **数据库连接** - 数据库连接配置
- **Pipeline** - 数据管道定义
- **Workflow** - 工作流定义
- **数据集** - 测试数据集定义
- **执行配置** - 运行环境配置
- **分区模式** - 数据分区策略
- **文件定义** - 文件格式定义

**存储方式：**
- 文件系统（JSON/XML）
- 数据库存储
- REST API 访问
- 版本控制集成

### 4.4 插件系统

Hop 采用基于注解的插件系统，支持动态加载和扩展。

**插件类型：**
- `@Transform` - 数据转换插件
- `@Action` - 工作流动作插件
- `@Database` - 数据库插件
- `@GuiElement` - UI 组件插件
- `@ExtensionPoint` - 扩展点插件

**插件发现：**
- Jandex 注解扫描
- 类路径动态加载
- 插件注册表管理
- 依赖隔离机制

**扩展示例：**
```java
@Plugin(
    id = "MyTransform",
    name = "My Transform",
    description = "Custom transform example",
    image = "myicon.svg",
    category = "Custom"
)
public class MyTransform extends BaseTransform<MyMeta, MyData> {
    // 实现逻辑
}
```

### 4.5 变量系统

强大的变量系统支持参数化和环境适配。

**变量类型：**
- 环境变量 - 系统环境变量
- JVM 属性 - Java 虚拟机参数
- 项目变量 - 项目级别配置
- 作业变量 - Job 执行时定义
- 管道变量 - Pipeline 执行时定义

**使用示例：**
```
${USER_HOME}      // 系统变量
${project_name}  // 项目变量
${internal_var}   // 内部变量
```

---

## 五、执行模式

### 5.1 本地执行
- **Hop GUI** - 图形界面直接执行
- **Hop Run** - 命令行执行 Pipeline/Workflow
- **Embedded API** - 嵌入式代码执行

### 5.2 远程执行
- **Hop Server** - 远程服务器执行
- **REST API** - 通过 HTTP 调用执行
- **Cluster Mode** - 集群分布式执行

### 5.3 Beam 引擎
支持通过 Apache Beam 运行，实现：
- Flink runner - 实时流处理
- Spark runner - 批处理和流处理
- Direct runner - 本地测试

---

## 六、部署方式

### 6.1 桌面客户端
```
cd assemblies/client/target
unzip hop-client-*.zip
cd hop
./hop-gui.sh    # Linux/Mac
hop-gui.bat      # Windows
```

### 6.2 Web 服务
- Hop Web - 现代化的 Web 界面
- Hop Server - 远程执行服务器
- REST API - 集成到其他系统

### 6.3 容器化
- Docker 镜像支持
- Docker Compose 编排
- Kubernetes Helm Charts

---

## 七、开发与扩展

### 7.1 构建命令

```bash
# 完整构建
./mvnw clean install

# 跳过测试
./mvnw clean install -DskipTests

# 构建特定模块
./mvnw clean install -pl core
```

### 7.2 代码规范

```bash
# 代码格式化（Spotless + Google Java Format）
mvn spotless:apply

# 代码检查（Checkstyle）
mvn checkstyle:check
```

### 7.3 自定义插件开发

创建自定义插件的步骤：
1. 创建 Maven 模块
2. 继承相应的基类（BaseTransform/BaseAction）
3. 使用 `@Plugin` 注解
4. 实现元数据和 UI 组件
5. 注册到插件系统

### 7.4 扩展点

通过 `@ExtensionPoint` 注解可扩展：
- 启动事件
- 执行前后事件
- 元数据变更事件
- UI 自定义事件

---

## 八、项目特色

1. **完全开源** - Apache 2.0 许可证
2. **跨平台** - 支持 Windows、Linux、macOS
3. **可视化设计** - 直观的拖拽式界面
4. **高度可扩展** - 插件架构支持无限扩展
5. **企业级特性** - 完整的权限、审计、日志
6. **大数据集成** - 原生支持 Hadoop、Spark、Beam
7. **多语言支持** - 内置 i18n 机制
8. **活跃社区** - Apache 社区持续维护和更新

---

## 九、典型应用场景

1. **数据仓库 ETL** - 从多个数据源抽取、转换、加载数据
2. **数据同步** - 不同系统间数据实时/批量同步
3. **数据质量** - 数据清洗、验证、去重
4. **报表生成** - 定时生成和分发报表
5. **数据迁移** - 数据库迁移、格式转换
6. **API 数据集成** - 对接各种 REST/SOAP API
7. **大数据处理** - Hadoop/Spark 数据处理任务
8. **实时数据流** - 使用 Beam 进行流式数据处理

---

## 十、参考信息

- **官网**: https://hop.apache.org
- **GitHub**: https://github.com/apache/hop
- **文档**: https://hop.apache.org/manual/
- **社区**: dev@hop.apache.org
- **版本**: 2.16.0 (当前分支: feature/2.16.2.1)