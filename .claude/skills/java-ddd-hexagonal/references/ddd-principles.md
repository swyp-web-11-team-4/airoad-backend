# DDD Fundamentals for Java

This document covers the core DDD building blocks and architectural principles for Java + Spring Boot projects following hexagonal architecture.

**Java Version**: Java 17+ with records, sealed classes, pattern matching
**Adaptable**: Replace `{basePackage}` with your project's base package (e.g., `com.shop.ecommerce`, `io.example.app`)

## Table of Contents
- [Hexagonal Architecture Layers](#hexagonal-architecture-layers)
- [DDD Building Blocks](#ddd-building-blocks)
- [Value Objects with Records](#value-objects-with-records)
- [Domain Events](#domain-events)
- [Repository Pattern](#repository-pattern)
- [Naming Conventions](#naming-conventions)

## Hexagonal Architecture Layers

### Domain Layer (Pure Java)
- **Location**: `modules/{module}/domain/`
- **Dependencies**: ZERO Spring dependencies, only `libs:common`
- **Build**: Uses `conventions` plugin
- **Contains**:
  - `model/` - Aggregate roots and entities
  - `vo/` - Value objects (records)
  - `event/` - Domain events (records)
  - `exception/` - Domain-specific exceptions

### Application Layer (Business Logic)
- **Location**: `modules/{module}/application/`
- **Dependencies**: Domain layer, Spring annotations allowed
- **Build**: Uses `springBootConventions` plugin (bootJar disabled)
- **Contains**:
  - `port/in/command/` - Write use cases
  - `port/in/query/` - Read use cases
  - `port/out/` - Repository interfaces
  - `service/command/` - Command service implementations
  - `service/query/` - Query service implementations

### Adapter Layer (Infrastructure)
- **Location**: `modules/{module}/adapter/`
- **Dependencies**: Application layer, framework-specific
- **Build**: Uses `springBootConventions` plugin
- **Contains**:
  - `in/web/` - REST controllers, DTOs, API documentation
  - `in/event/` - Domain event listeners
  - `out/persistence/` - Persistence adapters (choose your tech)
  - `out/client/` - External service clients

## DDD Building Blocks

### Aggregate Root
Base class from `libs:common`:

```java
public abstract class AggregateRoot<ID extends ValueObject> extends DomainEntity<ID> {
    protected void registerEvent(DomainEvent event) {
        EventManager.add(event);
    }

    public List<DomainEvent> getEvents() {
        return EventManager.toListAndClear();
    }

    public void clearEvents() {
        EventManager.clear();
    }
}
```

**Pattern for domain models:**
```java
public class MyEntity extends AggregateRoot<MyEntityId> {
    private final PropertyType property;

    private MyEntity(MyEntityId entityId, PropertyType property) {
        super(entityId);
        this.property = property;
    }

    // Factory for new entities (publishes events)
    public static MyEntity create(PropertyType property) {
        var entity = new MyEntity(MyEntityId.generate(), property);
        entity.registerEvent(new MyEntityCreatedEvent(entity.getEntityId()));
        return entity;
    }

    // Factory for reconstitution (no events)
    public static MyEntity from(
        MyEntityId entityId,
        PropertyType property,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        var entity = new MyEntity(entityId, property);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    // Business methods (immutable updates)
    public MyEntity updateProperty(PropertyType newProperty) {
        var updated = new MyEntity(getEntityId(), newProperty);
        updated.setCreatedAt(getCreatedAt());
        updated.setUpdatedAt(getUpdatedAt());
        updated.registerEvent(new MyEntityUpdatedEvent(getEntityId()));
        return updated;
    }

    // Getters
    public PropertyType getProperty() { return property; }
}
```

### Domain Entity
For entities that are NOT aggregate roots:

```java
public class ChildEntity extends DomainEntity<ChildEntityId> {
    private final PropertyType property;

    private ChildEntity(ChildEntityId entityId, PropertyType property) {
        super(entityId);
        this.property = property;
    }

    public static ChildEntity create(PropertyType property) {
        return new ChildEntity(ChildEntityId.generate(), property);
    }

    public static ChildEntity from(
        ChildEntityId entityId,
        PropertyType property,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        var entity = new ChildEntity(entityId, property);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        return entity;
    }

    public PropertyType getProperty() { return property; }
}
```

## Value Objects with Records

### ID Value Objects (Records - Preferred)

```java
public record MyEntityId(UUID value) implements ValueObject, Serializable {
    public static MyEntityId generate() {
        return new MyEntityId(UUID.randomUUID());
    }

    public static MyEntityId from(String value) {
        return new MyEntityId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
```

**Why records:**
- Immutable by default
- Auto-generates equals(), hashCode(), toString()
- Concise syntax
- Type safety (can't mix different ID types)

### Validated Value Objects

```java
public record Email(String value) implements ValueObject {
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Compact constructor with validation
    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        if (value.length() > 255) {
            throw new IllegalArgumentException("Email must not exceed 255 characters");
        }
    }
}
```

### Composite Value Objects

```java
public record OAuthInfo(OAuthProvider provider, String providerId) implements ValueObject {
    public OAuthInfo {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("Provider ID cannot be blank");
        }
    }
}
```

### Collection Value Objects

```java
public record TechStack(List<String> values) implements ValueObject {
    public TechStack {
        if (values == null) {
            throw new IllegalArgumentException("Tech stack values cannot be null");
        }
        if (values.stream().anyMatch(s -> s == null || s.isBlank())) {
            throw new IllegalArgumentException("Tech stack items cannot be blank");
        }
        if (values.size() > 50) {
            throw new IllegalArgumentException("Tech stack cannot exceed 50 items");
        }
        // Make defensive copy
        values = List.copyOf(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
```

### Sealed Classes for Type-Safe Enums

```java
public sealed interface OrderStatus permits
    OrderStatus.Pending,
    OrderStatus.Confirmed,
    OrderStatus.Shipped,
    OrderStatus.Delivered,
    OrderStatus.Cancelled {

    record Pending() implements OrderStatus {}
    record Confirmed() implements OrderStatus {}
    record Shipped(String trackingNumber) implements OrderStatus {}
    record Delivered(LocalDateTime deliveredAt) implements OrderStatus {}
    record Cancelled(String reason) implements OrderStatus {}

    // Pattern matching example
    default String displayName() {
        return switch (this) {
            case Pending p -> "Pending";
            case Confirmed c -> "Confirmed";
            case Shipped s -> "Shipped (" + s.trackingNumber() + ")";
            case Delivered d -> "Delivered";
            case Cancelled c -> "Cancelled: " + c.reason();
        };
    }
}
```

## Domain Events

```java
public record MyEntityCreatedEvent(
    MyEntityId entityId,
    String additionalData
) implements DomainEvent {
    // Optional factory method
    public static MyEntityCreatedEvent of(MyEntityId entityId) {
        return new MyEntityCreatedEvent(entityId, null);
    }
}
```

**Event registration in domain:**
```java
public static MyEntity create(PropertyType property) {
    var entity = new MyEntity(MyEntityId.generate(), property);
    entity.registerEvent(new MyEntityCreatedEvent(entity.getEntityId(), null));
    return entity;
}
```

**Event publishing in repository adapter:**
```java
@Repository
public class MyEntityRepositoryAdapter implements MyEntityRepository {
    private final MyEntityJpaRepository jpaRepository;
    private final EventContextManager eventContextManager;
    private final DomainEventPublisher domainEventPublisher;

    public MyEntityRepositoryAdapter(
        MyEntityJpaRepository jpaRepository,
        EventContextManager eventContextManager,
        DomainEventPublisher domainEventPublisher
    ) {
        this.jpaRepository = jpaRepository;
        this.eventContextManager = eventContextManager;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public MyEntity save(MyEntity entity) {
        var jpaEntity = MyEntityMapper.toEntity(entity);
        var saved = jpaRepository.save(jpaEntity);

        // Publish accumulated events
        var events = eventContextManager.getDomainEventsAndClear();
        events.forEach(domainEventPublisher::publish);

        return MyEntityMapper.toDomain(saved);
    }
}
```

**Event listening in adapter:**
```java
@Component
public class MyEventListener {
    private static final Logger log = LoggerFactory.getLogger(MyEventListener.class);
    private final SomeUseCase someUseCase;

    public MyEventListener(SomeUseCase someUseCase) {
        this.someUseCase = someUseCase;
    }

    @EventListener
    @Transactional
    public void handle(MyEntityCreatedEvent event) {
        log.info("Handling MyEntityCreatedEvent: {}", event.entityId());
        someUseCase.execute(new SomeUseCase.Command(...));
    }
}
```

## Repository Pattern

### Port Interface (Application Layer)
```java
public interface MyEntityRepository extends Repository<MyEntity, MyEntityId> {
    // Custom query methods
    Optional<MyEntity> findByProperty(PropertyType property);
    boolean existsByProperty(PropertyType property);
}
```

### JPA Adapter (Adapter Layer)
```java
@Repository
public class MyEntityRepositoryAdapter implements MyEntityRepository {
    private static final Logger log = LoggerFactory.getLogger(MyEntityRepositoryAdapter.class);

    private final MyEntityJpaRepository jpaRepository;
    private final EventContextManager eventContextManager;
    private final DomainEventPublisher domainEventPublisher;

    public MyEntityRepositoryAdapter(
        MyEntityJpaRepository jpaRepository,
        EventContextManager eventContextManager,
        DomainEventPublisher domainEventPublisher
    ) {
        this.jpaRepository = jpaRepository;
        this.eventContextManager = eventContextManager;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public MyEntity save(MyEntity entity) {
        log.debug("Saving entity: {}", entity.getEntityId());

        var jpaEntity = MyEntityMapper.toEntity(entity);
        var saved = jpaRepository.save(jpaEntity);

        // Publish events
        var events = eventContextManager.getDomainEventsAndClear();
        events.forEach(domainEventPublisher::publish);

        return MyEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<MyEntity> findById(MyEntityId id) {
        log.debug("Finding entity by ID: {}", id);
        return jpaRepository.findById(id.value())
            .map(MyEntityMapper::toDomain);
    }
}
```

### Mapper Pattern
```java
public final class MyEntityMapper {
    private MyEntityMapper() {} // Prevent instantiation

    public static MyEntity toDomain(MyEntityJpaEntity entity) {
        return MyEntity.from(
            new MyEntityId(entity.getId()),
            new PropertyType(entity.getPropertyValue()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static MyEntityJpaEntity toEntity(MyEntity domain) {
        var entity = MyEntityJpaEntity.from(
            domain.getEntityId().value(),
            domain.getProperty().value()
        );
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
```

## Naming Conventions

### Packages
```
{basePackage}.{module}.{layer}.{sublayer}.{feature}
```

Examples (assuming `basePackage` = `com.shop.ecommerce`):
- `com.shop.ecommerce.order.domain.model`
- `com.shop.ecommerce.order.application.port.in.command`
- `com.shop.ecommerce.order.adapter.out.persistence.jpa`

Examples (assuming `basePackage` = `io.example.app`):
- `io.example.app.content.domain.post.model`
- `io.example.app.content.application.post.port.in.command`

### Classes

| Type | Pattern | Example |
|------|---------|---------|
| Aggregate Root | `{Name}` | `Post`, `Member` |
| Domain Entity | `{Name}` | `Profile`, `Comment` |
| Value Object | `{Name}` (record) | `Email`, `PostId` |
| Domain Event | `{Entity}{Action}Event` (record) | `PostCreatedEvent` |
| Domain Exception | `{Entity}Exception` or `{Entity}NotFoundException` | `PostNotFoundException` |
| Use Case | `{Action}{Entity}UseCase` | `CreatePostUseCase` |
| Facade | `{Entity}CommandFacade` / `{Entity}QueryFacade` | `PostCommandFacade` |
| Service (Use Case) | `{Action}{Entity}Service` | `CreatePostService` |
| Service (Facade) | `{Entity}CommandService` / `{Entity}QueryService` | `PostCommandService` |
| Controller | `{Entity}Controller` | `PostController` |
| API Interface | `{Entity}Api` | `PostApi` |
| DTO | `{Action}{Entity}Request/Response` (record) | `CreatePostRequest` |
| JPA Entity | `{Entity}JpaEntity` | `PostJpaEntity` |
| Redis Entity | `{Entity}RedisEntity` | `TokenRedisEntity` |
| JPA Repository | `{Entity}JpaRepository` | `PostJpaRepository` |
| Repository Adapter | `{Entity}RepositoryAdapter` | `PostRepositoryAdapter` |
| Mapper | `{Entity}Mapper` (final class) | `PostMapper` |

### Methods

| Context | Pattern | Example |
|---------|---------|---------|
| Domain factory (new) | `create()`, `register()`, `issue()` | `Post.create(...)` |
| Domain factory (reconstitution) | `from()` | `Post.from(...)` |
| Domain business logic | `update{Property}()`, `delete()`, `publish()` | `updateTitle(...)` |
| Use case entry point | `execute(command/query)` | `execute(new CreatePostUseCase.Command(...))` |
| Facade methods | `{action}()` returning use case | `createPost(): CreatePostUseCase` |
| Mapper | `toDomain()`, `toEntity()` | `PostMapper.toDomain(...)` |

### Files
All files match class names: `Post.java`, `PostId.java`, `CreatePostUseCase.java`, etc.

## Java 17+ Features

### Records for Immutability
```java
// Before: traditional class
public final class Email {
    private final String value;

    public Email(String value) { this.value = value; }
    public String getValue() { return value; }
    // equals(), hashCode(), toString() boilerplate...
}

// After: record
public record Email(String value) implements ValueObject {
    public Email {
        // Compact constructor with validation
        if (value.isBlank()) throw new IllegalArgumentException();
    }
}
```

### Sealed Classes for Type Safety
```java
public sealed interface PaymentMethod permits
    CreditCard, DebitCard, PayPal, BankTransfer {

    Money getAmount();
}

// Pattern matching in switch
String description = switch (payment) {
    case CreditCard cc -> "Credit card ending in " + cc.lastFourDigits();
    case DebitCard dc -> "Debit card";
    case PayPal pp -> "PayPal: " + pp.email();
    case BankTransfer bt -> "Bank transfer";
};
```

### Pattern Matching
```java
// instanceof with pattern matching
if (entity instanceof Order order && order.getStatus() == OrderStatus.PENDING) {
    order.cancel();
}

// Switch expressions with pattern matching
String status = switch (order.getStatus()) {
    case OrderStatus.Pending p -> "Awaiting confirmation";
    case OrderStatus.Confirmed c -> "Order confirmed";
    case OrderStatus.Shipped s -> "Shipped: " + s.trackingNumber();
    default -> "Unknown";
};
```

### Text Blocks for SQL/JSON
```java
String query = """
    SELECT o.id, o.customer_id, o.status
    FROM orders o
    WHERE o.status = ?
    ORDER BY o.created_at DESC
    """;
```
