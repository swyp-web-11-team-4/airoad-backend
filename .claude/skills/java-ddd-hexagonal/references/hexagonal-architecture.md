# Hexagonal Architecture (Ports & Adapters) for Java

This document explains the hexagonal architecture pattern for organizing Java + Spring Boot projects following Domain-Driven Design principles.

## Core Concept

**Hexagonal Architecture** (also called Ports & Adapters) separates business logic from technical infrastructure, making the system:
- **Testable**: Domain logic tests without infrastructure
- **Flexible**: Swap implementations without changing domain
- **Maintainable**: Clear boundaries and responsibilities

## The Three Layers

```
┌─────────────────────────────────────────┐
│           ADAPTER LAYER                  │  (Infrastructure)
│  ┌─────────────────────────────────┐    │
│  │     APPLICATION LAYER            │    │  (Orchestration)
│  │  ┌───────────────────────┐      │    │
│  │  │   DOMAIN LAYER        │      │    │  (Business Logic)
│  │  │   Pure Java           │      │    │
│  │  └───────────────────────┘      │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘

Dependencies flow INWARD only:
Domain ← Application ← Adapter
```

### 1. Domain Layer (Core)

**Purpose**: Pure business logic and domain models

**Characteristics**:
- NO framework dependencies (no Spring, no JPA annotations)
- Pure Java classes
- Business rules and invariants
- Domain events

**Contains**:
- Aggregate Roots
- Entities
- Value Objects (records)
- Domain Events (records)
- Domain Exceptions

**Example**:
```java
// Pure domain model - NO Spring, NO JPA
public class Order extends AggregateRoot<OrderId> {
    private final CustomerId customerId;
    private final List<OrderItem> items;
    private final OrderStatus status;

    private Order(OrderId entityId, CustomerId customerId,
                  List<OrderItem> items, OrderStatus status) {
        super(entityId);
        this.customerId = customerId;
        this.items = List.copyOf(items);
        this.status = status;
    }

    public static Order place(CustomerId customerId, List<OrderItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must have items");
        }
        var order = new Order(OrderId.generate(), customerId, items, OrderStatus.PENDING);
        order.registerEvent(new OrderPlacedEvent(order.getEntityId()));
        return order;
    }

    public Order cancel() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Can only cancel pending orders");
        }
        var cancelled = new Order(getEntityId(), customerId, items, OrderStatus.CANCELLED);
        cancelled.setCreatedAt(getCreatedAt());
        cancelled.setUpdatedAt(getUpdatedAt());
        cancelled.registerEvent(new OrderCancelledEvent(getEntityId()));
        return cancelled;
    }

    // Getters
    public CustomerId getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return items; }
    public OrderStatus getStatus() { return status; }
}
```

### 2. Application Layer (Orchestration)

**Purpose**: Coordinate domain objects and define use cases

**Characteristics**:
- Depends on domain layer
- Spring annotations allowed (`@Service`, `@Transactional`)
- Orchestrates domain logic
- Transaction boundaries

**Contains**:
- **Ports (Interfaces)**:
  - `port/in/` - Inbound ports (use cases)
  - `port/out/` - Outbound ports (repositories, external services)
- **Services**: Implement use cases

**Inbound Port Example** (Driving):
```java
public interface PlaceOrderUseCase {
    Response execute(Command command);

    record Command(
        String customerId,
        List<OrderItemDto> items
    ) {}

    record Response(String orderId) {}
}
```

**Outbound Port Example** (Driven):
```java
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
    void deleteById(OrderId id);
}
```

**Service Implementation**:
```java
@Service
public class PlaceOrderService implements PlaceOrderUseCase {
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public PlaceOrderService(OrderRepository orderRepository,
                            InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional
    public Response execute(Command command) {
        // 1. Convert primitives to domain types
        var customerId = CustomerId.from(command.customerId());
        var items = command.items().stream()
            .map(dto -> new OrderItem(
                ProductId.from(dto.productId()),
                new Quantity(dto.quantity()),
                new Money(dto.price())
            ))
            .toList();

        // 2. Check inventory (outbound port)
        inventoryService.reserve(items);

        // 3. Create order (domain logic)
        var order = Order.place(customerId, items);

        // 4. Persist (outbound port)
        var saved = orderRepository.save(order);

        // 5. Return response
        return new Response(saved.getEntityId().toString());
    }
}
```

### 3. Adapter Layer (Infrastructure)

**Purpose**: Implement ports with concrete technology

**Characteristics**:
- Depends on application layer
- Framework-specific code
- Technical implementations

**Contains**:
- **Inbound Adapters** (Driving):
  - REST controllers
  - GraphQL resolvers
  - Message listeners
  - CLI commands

- **Outbound Adapters** (Driven):
  - Persistence implementations (JPA, Redis, MongoDB, etc.)
  - External API clients
  - Message publishers
  - File system access

**Inbound Adapter Example** (REST):
```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final PlaceOrderUseCase placeOrderUseCase;

    public OrderController(PlaceOrderUseCase placeOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody PlaceOrderRequest request) {
        var response = placeOrderUseCase.execute(
            new PlaceOrderUseCase.Command(
                request.customerId(),
                request.items()
            )
        );
        return ResponseEntity.ok(new OrderResponse(response.orderId()));
    }
}
```

**Outbound Adapter Example** (Persistence):
```java
@Repository
public class OrderRepositoryAdapter implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    private final DomainEventPublisher eventPublisher;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository,
                                  DomainEventPublisher eventPublisher) {
        this.jpaRepository = jpaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Order save(Order order) {
        // Convert domain → persistence model
        var entity = OrderMapper.toEntity(order);
        var saved = jpaRepository.save(entity);

        // Publish domain events
        order.getEvents().forEach(eventPublisher::publish);

        // Convert persistence → domain model
        return OrderMapper.toDomain(saved);
    }
}
```

## Dependency Rule

**CRITICAL**: Dependencies always point INWARD

```
✅ ALLOWED:
Adapter → Application → Domain
Application → Domain

❌ FORBIDDEN:
Domain → Application
Domain → Adapter
Application → Adapter (wrong direction)
```

## Ports vs Adapters

### Ports (Interfaces)

**Located in**: Application layer

**Purpose**: Define contracts

**Types**:
- **Inbound Ports**: What the application offers (use cases)
- **Outbound Ports**: What the application needs (repositories, services)

**Example**:
```java
// Inbound port
public interface CreateOrderUseCase { ... }

// Outbound port
public interface OrderRepository { ... }
public interface PaymentGateway { ... }
```

### Adapters (Implementations)

**Located in**: Adapter layer

**Purpose**: Implement ports with concrete technology

**Types**:
- **Inbound Adapters**: External → Application (REST, GraphQL, CLI)
- **Outbound Adapters**: Application → External (DB, APIs, Files)

**Example**:
```java
// Inbound adapter (implements use case interface - NOT RECOMMENDED)
// Better: Controller calls use case
@RestController
public class OrderRestController {
    private final CreateOrderUseCase useCase;
    // ...
}

// Outbound adapter (implements repository port)
@Repository
public class OrderJpaAdapter implements OrderRepository {
    private final OrderJpaRepository jpaRepo;
    // ...
}
```

## Module Structure

```
modules/{module}/
├── domain/                    # Pure domain logic
│   └── src/main/java/
│       ├── model/             # Aggregates, entities
│       ├── vo/                # Value objects (records)
│       ├── event/             # Domain events (records)
│       └── exception/         # Domain exceptions
│
├── application/               # Use cases & ports
│   └── src/main/java/
│       ├── port/
│       │   ├── in/            # Inbound ports (use cases)
│       │   └── out/           # Outbound ports (repositories)
│       └── service/           # Use case implementations
│
└── adapter/                   # Infrastructure
    ├── in/                    # Inbound adapters
    │   ├── web/               # REST controllers
    │   ├── graphql/           # GraphQL resolvers (if needed)
    │   └── event/             # Event listeners
    └── out/                   # Outbound adapters
        ├── persistence/       # Database adapters
        │   ├── jpa/           # JPA implementation
        │   ├── redis/         # Redis implementation
        │   └── mongo/         # MongoDB implementation
        └── client/            # External API clients
```

## Benefits

### 1. **Testability**
- Test domain logic without infrastructure
- Mock ports easily
- Fast unit tests

### 2. **Flexibility**
- Swap persistence (JPA → MongoDB)
- Multiple adapters for same port (REST + GraphQL)
- Technology decisions deferred

### 3. **Maintainability**
- Clear boundaries
- Single responsibility
- Easy to understand

### 4. **Team Scalability**
- Teams work on different adapters
- Domain experts focus on domain layer
- Infrastructure teams on adapters

## Common Patterns

### Pattern 1: Mapper Between Layers

**Problem**: Domain models ≠ Persistence models

**Solution**: Explicit mappers

```java
public final class OrderMapper {
    private OrderMapper() {} // Prevent instantiation

    public static Order toDomain(OrderJpaEntity entity) {
        return Order.from(
            new OrderId(entity.getId()),
            new CustomerId(entity.getCustomerId()),
            entity.getItems().stream()
                .map(OrderItemMapper::toDomain)
                .toList(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static OrderJpaEntity toEntity(Order domain) {
        var entity = new OrderJpaEntity();
        entity.setId(domain.getEntityId().value());
        entity.setCustomerId(domain.getCustomerId().value());
        entity.setStatus(domain.getStatus());
        entity.setItems(domain.getItems().stream()
            .map(OrderItemMapper::toEntity)
            .toList());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
```

### Pattern 2: Facade for Use Cases

**Problem**: Too many use case injections in adapters

**Solution**: Facade pattern

```java
public interface OrderFacade {
    PlaceOrderUseCase placeOrder();
    CancelOrderUseCase cancelOrder();
    UpdateOrderUseCase updateOrder();
}

@Service
public class OrderFacadeImpl implements OrderFacade {
    private final PlaceOrderUseCase placeOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final UpdateOrderUseCase updateOrderUseCase;

    public OrderFacadeImpl(PlaceOrderUseCase placeOrderUseCase,
                          CancelOrderUseCase cancelOrderUseCase,
                          UpdateOrderUseCase updateOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.updateOrderUseCase = updateOrderUseCase;
    }

    @Override
    public PlaceOrderUseCase placeOrder() { return placeOrderUseCase; }

    @Override
    public CancelOrderUseCase cancelOrder() { return cancelOrderUseCase; }

    @Override
    public UpdateOrderUseCase updateOrder() { return updateOrderUseCase; }
}

@RestController
public class OrderController {
    private final OrderFacade orderFacade;

    public OrderController(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    @PostMapping
    public ResponseEntity<?> place(@RequestBody PlaceOrderRequest request) {
        var response = orderFacade.placeOrder().execute(...);
        return ResponseEntity.ok(response);
    }
}
```

### Pattern 3: CQRS-lite (Command/Query Separation)

**Problem**: Mixing read and write concerns

**Solution**: Separate command and query use cases

```
application/port/in/
├── command/    # Write operations
└── query/      # Read operations
```

### Pattern 4: Anti-Corruption Layer

**Problem**: External APIs don't match domain model

**Solution**: Adapter translates external model → domain model

```java
@Component
public class ExternalPaymentAdapter implements PaymentGateway {
    private final PaymentApi externalApi;

    public ExternalPaymentAdapter(PaymentApi externalApi) {
        this.externalApi = externalApi;
    }

    @Override
    public PaymentResult charge(Payment payment) {
        // Translate domain → external format
        var request = toExternalFormat(payment);

        // Call external service
        var response = externalApi.charge(request);

        // Translate external → domain format
        return toDomainResult(response);
    }

    private ExternalPaymentRequest toExternalFormat(Payment payment) {
        return new ExternalPaymentRequest(
            payment.getAmount().getValue().toString(),
            payment.getCurrency().getCode(),
            payment.getDescription()
        );
    }

    private PaymentResult toDomainResult(ExternalPaymentResponse response) {
        return new PaymentResult(
            PaymentId.from(response.getId()),
            PaymentStatus.valueOf(response.getStatus())
        );
    }
}
```

## Pitfalls to Avoid

### ❌ Domain Depends on Framework
```java
// BAD: Domain entity with JPA annotations
@Entity
@Table(name = "orders")
public class Order extends AggregateRoot<OrderId> {
    @Id
    private UUID id;
    // ...
}
```

### ❌ Application Depends on Adapter
```java
// BAD: Service uses JPA entity directly
@Service
public class OrderService {
    private final OrderJpaRepository jpaRepo;
    // Should depend on OrderRepository interface instead
}
```

### ❌ Skipping Ports
```java
// BAD: Controller calls repository directly
@RestController
public class OrderController {
    private final OrderJpaRepository orderRepository;
    // Should call use case instead
}
```

### ✅ Correct Dependencies
```java
// GOOD: Each layer depends only on inner layers
Domain: Pure Java
Application: Domain + Spring
Adapter: Application + Infrastructure
```

## Summary

**Hexagonal Architecture** = **Ports & Adapters**

- **Ports**: Interfaces defining contracts
- **Adapters**: Concrete implementations
- **Dependency Rule**: Always point inward
- **Goal**: Flexible, testable, maintainable systems

Read [ddd-principles.md](ddd-principles.md) for DDD building blocks and [layer-responsibilities.md](layer-responsibilities.md) for specific layer guidelines.
