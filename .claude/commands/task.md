---
argument-hint: [description] or [issue-number]
description: Implement a complete feature with tests and quality checks
---

## Your task

Implement a feature end-to-end following the project's standards and architecture.

### 1. Understand Requirements

**If input is a number** (e.g., `123` or `#123`):
- Fetch the GitHub issue with `gh issue view {number}`
- Use the issue title and description as the specification

**If input is text**:
- Use it as the task specification

### 2. Implement the Feature

Design and implement the feature following the project architecture (see `CLAUDE.md`):

- **API First**: Design the REST endpoints and contracts
- **Layered Implementation**: Follow the domain-driven package structure
  - Domain entities and DTOs
  - Repository interfaces (infrastructure)
  - Use case interfaces and services (application)
  - REST controllers with OpenAPI docs (presentation)
- **Test Code**: Write tests achieving coverage per class
  - Use JUnit5 with nested structure and Given-When-Then pattern
  - Mock external dependencies appropriately

Use the project's specialized agents (`@api-developer`, `@backend-developer`, `@testing-engineer`) as needed - orchestrate them in whatever order makes sense for the task.

### 3. Quality Gates

Before completion, ensure:

```bash
./gradlew spotlessApply               # Format code
./gradlew check                       # Verify code
./gradlew build                       # Compile and run tests
```

All checks must pass.

## Important

- Adapt your approach based on task complexity
- Iterate as needed - don't force linear execution
- Follow architecture patterns
- Ensure all code is formatted and tested before completion
