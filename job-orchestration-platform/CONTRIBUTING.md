# Contributing to Job Orchestration Platform

Thank you for your interest in contributing to this project! This document provides guidelines for contributing.

## Development Setup

### Prerequisites

1. **Java 17+**: Download from [Adoptium](https://adoptium.net/) or use SDKMAN
2. **Maven 3.8+**: Download from [Apache Maven](https://maven.apache.org/)
3. **Docker Desktop**: Download from [Docker](https://www.docker.com/products/docker-desktop/)
4. **Git**: Download from [Git-SCM](https://git-scm.com/)

### Local Development

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/job-orchestration-platform.git
   cd job-orchestration-platform
   ```

2. Start infrastructure services:
   ```bash
   docker-compose up -d kafka zookeeper redis postgres
   ```

3. Build and run orchestrator:
   ```bash
   cd orchestrator-service
   mvn clean package -DskipTests
   java -jar target/orchestrator-service-1.0.0.jar
   ```

4. Build and run worker (in a new terminal):
   ```bash
   cd worker-service
   mvn clean package -DskipTests
   java -jar target/worker-service-1.0.0.jar
   ```

## Code Style

### Java Code Style

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use meaningful variable and method names
- Add Javadoc for public classes and methods
- Keep methods focused and under 30 lines when possible

### Commit Messages

Use conventional commit format:

```
type(scope): description

[optional body]

[optional footer]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

Examples:
```
feat(worker): add retry backoff configuration
fix(orchestrator): handle null payload in job creation
docs(readme): update installation instructions
```

## Pull Request Process

1. **Fork the repository** and create your branch from `main`
2. **Write tests** for any new functionality
3. **Update documentation** as needed
4. **Ensure all tests pass**: `mvn test`
5. **Submit a pull request** with a clear description

### PR Checklist

- [ ] Code follows project style guidelines
- [ ] Tests added/updated for changes
- [ ] Documentation updated
- [ ] Commit messages follow convention
- [ ] No merge conflicts

## Testing

### Running Tests

```bash
# All tests
mvn test

# Specific service
cd orchestrator-service && mvn test
cd worker-service && mvn test
```

### Integration Tests

Integration tests require running infrastructure:

```bash
docker-compose up -d kafka zookeeper redis postgres
mvn verify -Pintegration-test
```

## Reporting Issues

### Bug Reports

Include:
1. Description of the bug
2. Steps to reproduce
3. Expected behavior
4. Actual behavior
5. Environment details (OS, Java version, Docker version)
6. Relevant logs

### Feature Requests

Include:
1. Clear description of the feature
2. Use case / motivation
3. Proposed implementation (optional)

## Project Structure

```
job-orchestration-platform/
├── orchestrator-service/    # Job management API
├── worker-service/          # Job execution engine
├── docs/                    # Documentation
├── scripts/                 # Helper scripts
└── docker-compose.yml       # Infrastructure setup
```

## Questions?

Feel free to open an issue for any questions about contributing.

---

Thank you for contributing!
