# REVINCI™ DEVELOPMENT PLATFORM

This is a multi-module Gradle project for the Revinci Platform - a Java Spring Boot application with microservices architecture.

## Project Structure
- `commons/` - Shared libraries and common functionality
- `services/` - Microservices (platform-service, tenant-service, notification-service, etc.)
- `utilities/` - Utility applications and tools

## Common Commands
- Build project: `./gradlew build`
- Run tests: `./gradlew test`
- Clean build: `./gradlew clean build`

## Technology Stack
- Java with Spring Boot
- Gradle build system
- Multi-tenancy support
- Database provisioning (Azure/GCP)
- Keycloak for IAM
- Redis for caching
- PostgreSQL databases

## Development Notes
- Uses Lombok for reducing boilerplate code
- Multi-tenant architecture with tenant-aware entities
- Microservices communicate via messaging
- Comprehensive error handling and internationalization