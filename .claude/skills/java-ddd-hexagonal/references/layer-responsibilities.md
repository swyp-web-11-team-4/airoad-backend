# Layer Responsibilities for Java

This document defines the specific responsibilities and constraints for each layer in hexagonal architecture for Java + Spring Boot projects.

## Table of Contents
- [Domain Layer](#domain-layer)
- [Application Layer](#application-layer)
- [Adapter Layer](#adapter-layer)
- [Build Configuration](#build-configuration)

## Domain Layer

**Location**: `modules/{module}/domain/`

### Responsibilities

✅ **SHOULD DO**:
- Define business concepts (Aggregates, Entities, Value Objects)
- Enforce business rules and invariants
- Raise domain events for state changes
- Contain domain logic (validation, calculations, transformations)
- Define domain exceptions

✅ **MAY DO**:
- Use standard library features (collections, time, UUID, etc.)
- Define enums and sealed classes
- Use Java records for value objects
- Use pattern matching (Java 17+)

❌ **MUST NOT DO**:
- Use Spring annotations (@Service, @Entity, @Transactional, etc.)
- Use JPA annotations (@Id, @Column, @Table, etc.)
- Use any framework-specific annotations
- Depend on infrastructure concerns (databases, HTTP, etc.)
- Call external services directly
- Handle transactions

### Dependencies

**Allowed**:
- Java standard library (java.util, java.time, etc.)
- Internal domain common library (base classes, interfaces)
- Logging (SLF4J or similar)

**Forbidden**:
- Spring Framework
- JPA/Hibernate
- Redis
- Any infrastructure library

### Example: Domain Model

```java
// ✅ GOOD: Pure domain model
package com.example.project.order.domain.model;

import com.example.project.common.domain.AggregateRoot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Order extends AggregateRoot<OrderId> {
    private final CustomerId customerId;
    private final List<OrderItem> items;
    private final OrderStatus status;

    private Order(OrderId entityId, CustomerId customerId,
                  List<OrderItem> items, OrderStatus status) {
        super(entityId);
        this.customerId = customerId;
        this.items = List.copyOf(items);  // Defensive copy
        this.status = status;
    }

    public static Order place(CustomerId customerId, List<OrderItem> items) {
        // Business rule enforcement
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        if (items.stream().anyMatch(item -> item.getQuantity().value() <= 0)) {
            throw new IllegalArgumentException("All items must have positive quantity");
        }

        var order = new Order(
            OrderId.generate(),
            customerId,
            items,
            OrderStatus.PENDING
        );

        // Raise domain event
        order.registerEvent(new OrderPlacedEvent(order.getEntityId(), customerId));

        return order;
    }

    public Money calculateTotal() {
        return items.stream()
            .map(item -> item.getPrice().multiply(item.getQuantity()))
            .reduce(Money.ZERO, Money::add);
    }

    public Order cancel() {
        // Business rule
        if (!status.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new IllegalStateException(
                "Cannot cancel order in " + status + " status"
            );
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

// ❌ BAD: Domain model with JPA annotations
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
    // ...
}
```

### Build Configuration

```kotlin
// domain/build.gradle.kts
plugins {
    id("conventions")  // Pure Java, NO Spring
}

dependencies {
    api(project(":libs:common"))  // Domain base classes
    testImplementation(libs.bundles.java.test)
}
```

---

## Application Layer

**Location**: `modules/{module}/application/`

### Responsibilities

✅ **SHOULD DO**:
- Define use cases (application services)
- Define port interfaces (inbound and outbound)
- Orchestrate domain objects
- Define transaction boundaries
- Convert between domain and application DTOs
- Coordinate multiple domain operations

✅ **MAY DO**:
- Use Spring annotations (@Service, @Transactional, @Component)
- Inject dependencies via constructor
- Use logging
- Call outbound ports (repositories, external services)

❌ **MUST NOT DO**:
- Contain business logic (that belongs in domain)
- Use infrastructure-specific code (JPA queries, HTTP calls, etc.)
- Depend on adapter layer
- Know about persistence details
- Know about HTTP/REST details

### Dependencies

**Allowed**:
- Domain layer
- Spring Framework (annotations, DI, transactions)
- Common libraries

**Forbidden**:
- Adapter layer
- JPA/Hibernate entities
- REST/HTTP libraries
- Infrastructure implementations

### Port Interfaces

#### Inbound Port (Use Case)

```java
// ✅ GOOD: Use case interface with nested Command/Response records
package com.example.project.order.application.port.in;

import java.math.BigDecimal;
import java.util.List;

public interface PlaceOrderUseCase {
    Response execute(Command command);

    record Command(
        String customerId,
        List<OrderItemDto> items
    ) {}

    record Response(
        String orderId,
        String status,
        BigDecimal total
    ) {}
}

public record OrderItemDto(
    String productId,
    int quantity,
    BigDecimal price
) {}
```

#### Outbound Port (Repository)

```java
// ✅ GOOD: Repository port interface (no implementation)
package com.example.project.order.application.port.out;

import com.example.project.order.domain.model.Order;
import com.example.project.order.domain.vo.OrderId;
import com.example.project.order.domain.vo.CustomerId;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
    List<Order> findByCustomerId(CustomerId customerId);
    void deleteById(OrderId id);
}

// ✅ GOOD: External service port
public interface PaymentGateway {
    PaymentResult processPayment(Payment payment);
}

// ✅ GOOD: Event publisher port
public interface EventPublisher {
    void publish(DomainEvent event);
}
```

### Service Implementation

```java
// ✅ GOOD: Service orchestrates domain logic
package com.example.project.order.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceOrderService implements PlaceOrderUseCase {
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentGateway paymentGateway;

    public PlaceOrderService(
        OrderRepository orderRepository,
        InventoryService inventoryService,
        PaymentGateway paymentGateway
    ) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.paymentGateway = paymentGateway;
    }

    @Override
    @Transactional
    public Response execute(Command command) {
        // 1. Convert primitives to value objects
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

        // 3. Domain logic
        var order = Order.place(customerId, items);

        // 4. Process payment (outbound port)
        var payment = new Payment(order.calculateTotal());
        paymentGateway.processPayment(payment);

        // 5. Persist (outbound port)
        var saved = orderRepository.save(order);

        // 6. Return response
        return new Response(
            saved.getEntityId().toString(),
            saved.getStatus().name(),
            saved.calculateTotal().getAmount()
        );
    }
}
```

### Build Configuration

```kotlin
// application/build.gradle.kts
plugins {
    id("springBootConventions")  // Spring Boot, bootJar disabled
}

dependencies {
    api(project(":modules:order:domain"))  // Expose domain
    implementation(project(":libs:common"))

    implementation(libs.spring.boot.starter.core)
    testImplementation(libs.bundles.java.test)
    testImplementation(libs.bundles.spring.boot.test)
}
```

---

## Adapter Layer

**Location**: `modules/{module}/adapter/`

### Responsibilities

✅ **SHOULD DO**:
- Implement port interfaces from application layer
- Handle infrastructure concerns (HTTP, databases, messaging, etc.)
- Convert between external formats and application formats
- Handle framework-specific details
- Configure infrastructure

✅ **MAY DO**:
- Use any framework or library needed
- Use Spring annotations (@RestController, @Repository, @Component)
- Use JPA, Redis, MongoDB, etc.
- Call external APIs
- Handle HTTP requests/responses

❌ **MUST NOT DO**:
- Contain business logic (that belongs in domain)
- Contain orchestration logic (that belongs in application)
- Depend on other adapters directly

### Inbound Adapters

#### REST Controller

```java
// ✅ GOOD: Controller implements inbound adapter
package com.example.project.order.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final PlaceOrderUseCase placeOrderUseCase;

    public OrderController(PlaceOrderUseCase placeOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody PlaceOrderRequest request) {
        // Convert REST DTO → Use case command
        var command = new PlaceOrderUseCase.Command(
            request.customerId(),
            request.items().stream()
                .map(item -> new OrderItemDto(
                    item.productId(),
                    item.quantity(),
                    item.price()
                ))
                .toList()
        );

        // Execute use case
        var response = placeOrderUseCase.execute(command);

        // Convert use case response → REST DTO
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(new OrderResponse(
                response.orderId(),
                response.status(),
                response.total()
            ));
    }
}

// REST DTOs (records)
record PlaceOrderRequest(
    String customerId,
    List<OrderItemRequest> items
) {}

record OrderItemRequest(
    String productId,
    int quantity,
    BigDecimal price
) {}

record OrderResponse(
    String orderId,
    String status,
    BigDecimal total
) {}
```

### Outbound Adapters

#### Persistence Adapter

```java
// ✅ GOOD: Repository adapter implements outbound port
package com.example.project.order.adapter.out.persistence.jpa;

import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    private final DomainEventPublisher eventPublisher;

    public OrderRepositoryAdapter(
        OrderJpaRepository jpaRepository,
        DomainEventPublisher eventPublisher
    ) {
        this.jpaRepository = jpaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Order save(Order order) {
        // Convert domain → JPA entity
        var entity = OrderMapper.toEntity(order);

        // Persist
        var saved = jpaRepository.save(entity);

        // Publish domain events
        order.getEvents().forEach(eventPublisher::publish);

        // Convert JPA entity → domain
        return OrderMapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value())
            .map(OrderMapper::toDomain);
    }
}

// JPA Entity (separate from domain model)
@Entity
@Table(name = "orders")
public class OrderJpaEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItemJpaEntity> items;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Constructors, getters, setters
}

// Mapper between domain and persistence
public final class OrderMapper {
    private OrderMapper() {} // Prevent instantiation

    public static Order toDomain(OrderJpaEntity entity) {
        return Order.from(
            new OrderId(entity.getId()),
            new CustomerId(entity.getCustomerId()),
            entity.getItems().stream()
                .map(OrderItemMapper::toDomain)
                .toList(),
            OrderStatus.valueOf(entity.getStatus()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static OrderJpaEntity toEntity(Order domain) {
        var entity = new OrderJpaEntity();
        entity.setId(domain.getEntityId().value());
        entity.setCustomerId(domain.getCustomerId().value());
        entity.setStatus(domain.getStatus().name());
        entity.setItems(
            domain.getItems().stream()
                .map(OrderItemMapper::toEntity)
                .toList()
        );
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
```

### Build Configuration

```kotlin
// adapter/in/web/build.gradle.kts
plugins {
    id("springBootConventions")
}

dependencies {
    implementation(project(":modules:order:domain"))
    implementation(project(":modules:order:application"))
    implementation(project(":libs:adapter:web"))

    implementation(libs.bundles.spring.boot.web)
    implementation(libs.springdoc.openapi)
    testImplementation(libs.bundles.spring.boot.test)
}

// adapter/out/persistence/jpa/build.gradle.kts
plugins {
    id("springBootConventions")
}

dependencies {
    implementation(project(":modules:order:domain"))
    implementation(project(":modules:order:application"))
    implementation(project(":libs:adapter:persistence:jpa"))

    implementation(libs.bundles.spring.boot.data)
    runtimeOnly(libs.postgresql)  // or any database driver
    testImplementation(libs.bundles.spring.boot.test)
}
```

---

## Build Configuration

### Convention Plugins

Use buildSrc convention plugins to standardize configuration:

**Domain layer**:
```kotlin
plugins {
    id("conventions")  // Pure Java, Checkstyle, NO Spring
}
```

**Application layer**:
```kotlin
plugins {
    id("springBootConventions")  // Spring Boot, bootJar disabled
}
```

**Adapter layer**:
```kotlin
plugins {
    id("springBootConventions")  // Spring Boot, bootJar disabled
}
```

**App module** (only module with executable JAR):
```kotlin
plugins {
    id("springBootConventions")  // Spring Boot, bootJar ENABLED
}
```

### Dependency Flow

```
app
 ↓ depends on
adapter (web, persistence, etc.)
 ↓ depends on
application
 ↓ depends on
domain
 ↓ depends on
common (shared libs)
```

---

## Summary

| Layer | Depends On | Contains | Framework | Persistence |
|-------|-----------|----------|-----------|-------------|
| **Domain** | common | Business logic, domain models | ❌ None | ❌ None |
| **Application** | domain, common | Use cases, ports | ✅ Spring | ❌ None |
| **Adapter** | application, domain, common | Controllers, repositories, clients | ✅ Any | ✅ Any |

**Key principle**: Each layer depends only on layers INSIDE it, never outside.

## Java-Specific Best Practices

### Use Records for DTOs and Value Objects
```java
// ✅ GOOD: Immutable by default
public record OrderId(UUID value) implements ValueObject {}

// ❌ BAD: Mutable class
public class OrderId {
    private UUID value;
    public void setValue(UUID value) { this.value = value; }
}
```

### Use Sealed Classes for Type-Safe Domain Models
```java
// ✅ GOOD: Exhaustive pattern matching
public sealed interface OrderStatus permits
    OrderStatus.Pending,
    OrderStatus.Confirmed,
    OrderStatus.Shipped {

    record Pending() implements OrderStatus {}
    record Confirmed() implements OrderStatus {}
    record Shipped(String trackingNumber) implements OrderStatus {}
}
```

### Use Final Classes to Prevent Inheritance
```java
// ✅ GOOD: Prevent unintended inheritance
public final class OrderMapper {
    private OrderMapper() {} // Utility class
}
```

### Use Constructor Injection
```java
// ✅ GOOD: Immutable dependencies
@Service
public class OrderService {
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}

// ❌ BAD: Field injection
@Service
public class OrderService {
    @Autowired
    private OrderRepository repository;
}
```
