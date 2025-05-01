# Vistora - Database Schema Analyzer

Vistora is a Spring Boot application that extracts database schema information and generates Java model classes with JPA annotations based on your database structure.

## Overview

This tool connects to your database, analyzes its structure (tables, columns, primary keys, foreign keys, and indices), and automatically generates corresponding Java entity classes that are ready to use with JPA/Hibernate.

## Technologies Used

- **Spring Boot** - Framework for creating standalone Spring applications
- **JDBC** - For database connectivity and metadata extraction
- **JavaPoet** - Library for Java source code generation
- **JPA/Hibernate** - For ORM annotations in the generated models
- **Lombok** - Reduces boilerplate code in the application models

## Architecture

The application follows a standard Spring MVC architecture:

```
┌────────────┐     ┌───────────┐     ┌────────────┐
│ Controller │────>│  Service  │────>│  Database  │
└────────────┘     └───────────┘     └────────────┘
       │                │                  │
       │                │                  │
       ▼                ▼                  ▼
┌────────────────────────────────────────────────┐
│         Generated Model Classes (.java)         │
└────────────────────────────────────────────────┘
```

## Core Components

### Models

- **DatabaseTable** - Represents a database table with associated metadata
- **DatabaseColumn** - Represents a column within a table
- **DatabaseForeignKey** - Represents a foreign key relationship
- **DatabaseIndex** - Represents a database index

### Services

- **SchemaService** - Extracts database schema information using JDBC's metadata API
- **ModelGeneratorService** - Generates Java model classes with JPA annotations using JavaPoet

### Controllers

- **SchemaController** - REST endpoints for schema extraction and model generation

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/schema/tables` | GET | Retrieve all database tables with their schema information |
| `/api/schema/tables/{tableName}` | GET | Retrieve schema information for a specific table |
| `/api/schema/generate-models` | GET | Generate model classes for all tables |
| `/api/schema/generate-model/{tableName}` | GET | Generate a model class for a specific table |

## Flow Diagram

```
┌────────────────────┐
│    Client Request  │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│  SchemaController  │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐     ┌─────────────────────┐
│   SchemaService    │────>│ DatabaseTable Model │
└─────────┬──────────┘     └─────────────────────┘
          │
          ▼
┌────────────────────┐
│ModelGeneratorService│
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│  Generated Java    │
│  Entity Classes    │
└────────────────────┘
```

## Type Mapping

The application maps SQL data types to Java types automatically:

- `VARCHAR`, `CHAR`, `TEXT` → `String`
- `INT`, `TINYINT`, `SMALLINT`, `MEDIUMINT` → `Integer`
- `BIGINT` → `Long`
- `DECIMAL`, `NUMERIC` → `BigDecimal`
- `FLOAT` → `Float`
- `DOUBLE` → `Double`
- `DATE` → `LocalDate`
- `DATETIME`, `TIMESTAMP` → `LocalDateTime`
- `TIME` → `LocalTime`
- `BOOLEAN`, `BIT` → `Boolean`
- `BLOB`, `LONGBLOB` → `byte[]`
- `JSON` → `String`

## Configuration

The database connection is configured using properties with the `database` prefix:

```properties
server:
    port: 8080

spring:
    application:
        name: ApplicationName
    datasource:
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://localhost:3306/db_name
        username: db_username
        password: db_password
```

## Generated Models

The generated model classes include:

- JPA entity annotations (`@Entity`, `@Table`)
- Column definitions with appropriate annotations
- Primary key and auto-increment annotations
- Getters and setters for all fields
- ToString method implementation

## Usage Example

1. Configure your database connection in `application.properties`
2. Start the application
3. Access `http://localhost:8080/api/schema/tables` to see your database schema
4. Access `http://localhost:8080/api/schema/generate-models` to generate all model classes
5. Find the generated classes in `src/main/java/com/tanay/vistora/generated/`