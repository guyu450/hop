# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Apache Hop is an open-source data orchestration platform (version 2.16.0) written in Java 17. It provides a visual interface for creating and managing ETL (Extract, Transform, Load) pipelines and workflows. The platform supports both batch and real-time data processing with a plugin-based architecture.

## Build Commands

### Building the Project

Use Maven to build the project:

```bash
# Standard Maven build
mvn clean install

# Using Maven wrapper (recommended)
./mvnw clean install

# Skip tests for faster builds
./mvnw clean install -DskipTests

# Build specific modules
./mvnw clean install -pl core
```

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl engine

# Run specific test class
mvn test -Dtest=PipelineMetaTest

# Run tests with verbose output
mvn test -X
```

### Code Quality

```bash
# Run Checkstyle
mvn checkstyle:check

# Run Spotless (code formatting)
mvn spotless:apply

# Run both checkstyle and spotless
mvn checkstyle:check spotless:apply
```

## Architecture

### Core Components

1. **Core (`/core`)**
   - Base classes and interfaces
   - Plugin system
   - Metadata management
   - Configuration management
   - Exception handling

2. **Engine (`/engine`)**
   - Pipeline execution engine
   - Workflow execution engine
   - Transform execution framework
   - Partitioning and distribution

3. **UI (`/ui`)**
   - Hop GUI application
   - Visual pipeline/workflow designer
   - Dialogs and components
   - Perspectives and views

4. **Plugins (`/plugins`)**
   - **Transforms**: Data processing components (150+ transforms)
   - **Actions**: Workflow action components
   - **Databases**: Database connection plugins
   - **Misc**: Various utility plugins

### Plugin Architecture

The plugin system is central to Hop's extensibility:

- **Plugin Registry**: Central registry for all plugins
- **Plugin Types**:
  - Transforms (data processing)
  - Actions (workflow steps)
  - Database connections
  - Custom GUI components
- **Plugin Loading**: Dynamic loading from JAR files
- **Extension Points**: Hook system for extending functionality

### Metadata System

Hop uses a metadata system to store:
- Pipeline definitions (`.hpl` files)
- Workflow definitions (`.hwf` files)
- Database connections
- Execution configurations
- Project settings

Metadata is stored in JSON format and can be managed through:
- File-based storage
- Database storage
- REST API

### Key Data Structures

- **PipelineMeta**: Definition of a pipeline with transforms, hops, and metadata
- **WorkflowMeta**: Definition of a workflow with actions and hops
- **RowSet**: Data transfer mechanism between pipeline components
- **DatabaseMeta**: Database connection metadata
- **Plugin**: Base class for all plugins

## Development Guidelines

### Adding New Transforms

1. Create a new module under `/plugins/transforms/`
2. Implement the `BaseTransform` class
3. Define metadata with annotations
4. Add UI components for configuration
5. Register the plugin in the POM

### Adding New Actions

1. Create a new module under `/plugins/actions/`
2. Implement the `BaseAction` interface
3. Define action-specific metadata
4. Add UI components for configuration
5. Handle action execution and result

### Database Plugin Development

1. Create module under `/plugins/databases/`
2. Implement `IDatabase` interface
3. Define database-specific metadata
4. Add UI for connection configuration
5. Implement SQL dialect handling

## File Locations

### Important Directories

- **Sample Data**: `/deployment/hop-data/config/projects/samples/`
- **Default Config**: `/assemblies/static/src/main/resources/config/`
- **Integration Tests**: `/integration-tests/`
- **Docker**: `/docker/`
- **Documentation**: `/docs/`

### Configuration Files

- **Project Config**: `project-config.json`
- **Pipeline Config**: `pipeline-run-configuration/local.json`
- **Workflow Config**: `workflow-run-configuration/local.json`

## Testing Strategy

### Unit Tests

- Located in `/src/test/java/`
- Use JUnit 5
- Mock dependencies with Mockito
- Test core functionality and edge cases

### Integration Tests

- Located in `/integration-tests/`
- Test complete pipelines/workflows
- Test with different databases and data sources
- Test REST API functionality

### Test Data

- Sample datasets in `/integration-tests/*/metadata/dataset/`
- Golden files for validation
- Configuration for various test environments

## Running the Application

After building:

```bash
# Navigate to client directory
cd assemblies/client/target

# Unzip the client
unzip hop-client-*.zip

# Run the GUI
cd hop
./hop-gui.sh  # Linux/Mac
hop-gui.bat   # Windows
```

## Common Development Tasks

1. **Adding a new database plugin**: Implement IDatabase interface, create UI components, add SQL dialect support
2. **Creating a custom transform**: Extend BaseTransform, define metadata, implement processing logic
3. **Adding a workflow action**: Implement BaseAction, define action parameters, handle execution flow
4. **Customizing the GUI**: Use GUI plugin annotations, create custom dialogs and views
5. **Metadata management**: Implement metadata handlers for custom storage backends

## Internationalization

Hop supports multiple languages:
- Localization files in `/i18n/`
- Use `BaseMessages.getString()` for internationalized strings
- Language selection through GUI or environment variables

## Security

- Plugin system supports sandboxing
- Database credentials encrypted at rest
- Row-level security in metadata
- Extension points for custom security providers