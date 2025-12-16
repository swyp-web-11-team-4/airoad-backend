# TDD Workflow for Java Projects

This document adapts Kent Beck's Test-Driven Development approach to Java tech stack: Java 17+, Spring Boot, JUnit 5, Mockito, and AssertJ.

## Table of Contents
- [Core Principles](#core-principles)
- [Red-Green-Refactor Cycle](#red-green-refactor-cycle)
- [Test Structure with JUnit 5](#test-structure-with-junit-5)
- [Testing Each Layer](#testing-each-layer)
- [Example TDD Session](#example-tdd-session)

## Core Principles

### 1. Red → Green → Refactor
- **Red**: Write a failing test that defines desired behavior
- **Green**: Write the minimum code to make the test pass
- **Refactor**: Improve structure while keeping tests green

### 2. Test-First Discipline
- Always write the test BEFORE implementing the feature
- Write ONE test at a time
- Make it run, then improve structure
- Never mix behavioral changes with structural changes

### 3. Minimal Implementation
- Implement only what's needed to pass the current test
- Resist the urge to add "future-proof" features
- Let the tests guide the design

### 4. Frequent Validation
- Run all tests after every change (except long-running tests)
- Commit only when all tests pass with no warnings
- Use `./gradlew check` to run tests + checkstyle + coverage

## Red-Green-Refactor Cycle

### Phase 1: RED (Write Failing Test)

**Choose the simplest next behavior to test:**
```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CreatePostServiceTest {
    @Test
    void shouldCreatePostWithValidTitleAndContent() {
        // This test will fail - we haven't implemented the service yet
        fail("Not implemented yet");
    }
}
```

**Run the test and verify it fails:**
```bash
./gradlew :modules:content:application:test --tests CreatePostServiceTest
```

### Phase 2: GREEN (Minimal Implementation)

**Write ONLY enough code to pass:**
```java
// Start with the simplest possible implementation
@Service
public class CreatePostService implements CreatePostUseCase {
    private final PostRepository postRepository;

    public CreatePostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    @Transactional
    public Response execute(Command command) {
        var post = Post.create(
            new Title(command.title()),
            new Content(command.content())
        );
        var saved = postRepository.save(post);
        return new Response(saved.getEntityId().value().toString());
    }
}
```

**Run the test and verify it passes:**
```bash
./gradlew :modules:content:application:test --tests CreatePostServiceTest
```

### Phase 3: REFACTOR (Improve Structure)

**Only after tests are green, refactor:**
- Extract duplicated code
- Rename for clarity
- Simplify complex logic
- Improve structure

**Run tests after EVERY refactoring:**
```bash
./gradlew :modules:content:application:test
```

## Test Structure with JUnit 5

### JUnit 5 with AssertJ Style

```java
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MyServiceTest {
    @Mock
    private MyRepository mockRepository;

    @InjectMocks
    private MyService service;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void shouldDoSomethingWhenConditionIsMet() {
        // Arrange
        var input = "test";
        when(mockRepository.findByProperty(any())).thenReturn(Optional.empty());

        // Act
        var result = service.doSomething(input);

        // Assert
        assertThat(result).isNotNull();
        verify(mockRepository, times(1)).findByProperty(input);
    }

    @Test
    void shouldThrowExceptionWhenInvalidInput() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> service.doSomething(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be blank");
    }
}
```

### Naming Tests

Use descriptive names that explain behavior:
- ✅ `shouldCreatePostWithValidTitleAndContent()`
- ✅ `shouldThrowPostNotFoundExceptionWhenPostDoesNotExist()`
- ✅ `shouldRegisterMemberCreatedEventWhenMemberIsCreated()`
- ❌ `test1()`
- ❌ `testCreatePost()`

### Test Organization

```java
@DisplayName("Order")
class OrderTest {

    @Nested
    @DisplayName("place()")
    class PlaceTest {
        @Test
        @DisplayName("should create order with pending status")
        void shouldCreateOrderWithPendingStatus() {
            // ...
        }

        @Test
        @DisplayName("should throw exception when items are empty")
        void shouldThrowExceptionWhenItemsAreEmpty() {
            // ...
        }
    }

    @Nested
    @DisplayName("cancel()")
    class CancelTest {
        @Test
        @DisplayName("should cancel pending order")
        void shouldCancelPendingOrder() {
            // ...
        }
    }
}
```

## Testing Each Layer

### Domain Layer (Pure Logic)

**No mocking needed - test pure functions:**
```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PostTest {
    @Test
    void shouldCreatePostWithTitleAndContent() {
        // Arrange
        var title = new Title("My Post");
        var content = new Content("Hello World");
        var authorId = MemberId.generate();

        // Act
        var post = Post.create(title, content, authorId);

        // Assert
        assertThat(post.getEntityId()).isNotNull();
        assertThat(post.getTitle()).isEqualTo(title);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.getStatus()).isEqualTo(PostStatus.DRAFT);
    }

    @Test
    void shouldPublishPostAndRegisterEvent() {
        // Arrange
        var post = Post.create(
            new Title("Test"),
            new Content("Test"),
            MemberId.generate()
        );

        // Act
        var published = post.publish();

        // Assert
        assertThat(published.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isNotNull();

        var events = published.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PostPublishedEvent.class);
    }

    @Test
    void shouldValidateTitleLength() {
        // Act & Assert
        assertThatThrownBy(() -> new Title(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Title cannot be blank");
    }
}
```

### Application Layer (Use Cases)

**Mock repository ports:**
```java
import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreatePostServiceTest {
    @Mock
    private PostRepository mockRepository;

    @InjectMocks
    private CreatePostService service;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void shouldCreatePostWithValidData() {
        // Arrange
        var command = new CreatePostUseCase.Command(
            "Test Post",
            "Test Content",
            UUID.randomUUID().toString()
        );

        var expectedPost = Post.create(
            new Title(command.title()),
            new Content(command.content()),
            MemberId.from(command.authorId())
        );

        when(mockRepository.save(any(Post.class))).thenReturn(expectedPost);

        // Act
        var response = service.execute(command);

        // Assert
        assertThat(response.postId()).isNotNull();
        verify(mockRepository, times(1)).save(any(Post.class));
    }

    @Test
    void shouldThrowExceptionWhenTitleIsBlank() {
        // Arrange
        var command = new CreatePostUseCase.Command(
            "",
            "Test",
            UUID.randomUUID().toString()
        );

        // Act & Assert
        assertThatThrownBy(() -> service.execute(command))
            .isInstanceOf(IllegalArgumentException.class);

        verify(mockRepository, never()).save(any());
    }
}
```

### Adapter Layer (Controllers, Repositories)

**Integration tests with Spring Boot Test:**
```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
class PostControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostCommandFacade postCommandFacade;

    @MockBean
    private CreatePostUseCase createPostUseCase;

    @Test
    void shouldCreatePostAndReturn201() throws Exception {
        // Arrange
        when(postCommandFacade.createPost()).thenReturn(createPostUseCase);
        when(createPostUseCase.execute(any()))
            .thenReturn(new CreatePostUseCase.Response(UUID.randomUUID().toString()));

        var requestBody = """
            {
              "title": "Integration Test",
              "content": "Test Content",
              "authorId": "550e8400-e29b-41d4-a716-446655440000"
            }
            """;

        // Act & Assert
        mockMvc.perform(
                post("/api/v1/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.postId").exists());
    }
}
```

**Repository adapter tests:**
```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import({PostRepositoryAdapter.class, /* event publishers */})
class PostRepositoryAdapterTest {
    @Autowired
    private PostRepositoryAdapter adapter;

    @Test
    void shouldSavePostAndPublishEvents() {
        // Arrange
        var post = Post.create(
            new Title("Test"),
            new Content("Test"),
            MemberId.generate()
        );

        // Act
        var saved = adapter.save(post);

        // Assert
        assertThat(saved.getEntityId()).isEqualTo(post.getEntityId());

        var found = adapter.findById(saved.getEntityId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo(post.getTitle());
    }
}
```

## Example TDD Session

### Scenario: Add "publish post" feature

**Step 1: Write domain test (RED)**
```java
@Test
void shouldPublishDraftPost() {
    var post = Post.create(
        new Title("Test"),
        new Content("Test"),
        MemberId.generate()
    );

    var published = post.publish();

    assertThat(published.getStatus()).isEqualTo(PostStatus.PUBLISHED);
    assertThat(published.getPublishedAt()).isNotNull();
}
```

**Run test → FAILS** ✗

**Step 2: Implement domain logic (GREEN)**
```java
public class Post extends AggregateRoot<PostId> {
    public Post publish() {
        if (status != PostStatus.DRAFT) {
            throw new IllegalStateException("Only draft posts can be published");
        }

        var updated = new Post(
            getEntityId(),
            title,
            content,
            authorId,
            PostStatus.PUBLISHED
        );
        updated.setPublishedAt(LocalDateTime.now());
        updated.setCreatedAt(getCreatedAt());
        updated.setUpdatedAt(getUpdatedAt());
        updated.registerEvent(new PostPublishedEvent(getEntityId()));
        return updated;
    }
}
```

**Run test → PASSES** ✓

**Step 3: Write application test (RED)**
```java
@Test
void shouldPublishPostWhenValidPostIdProvided() {
    var postId = PostId.generate();
    var command = new PublishPostUseCase.Command(postId.value().toString());

    var existingPost = Post.create(
        new Title("Test"),
        new Content("Test"),
        MemberId.generate()
    );
    when(mockRepository.findById(postId)).thenReturn(Optional.of(existingPost));
    when(mockRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.execute(command);

    assertThat(response.status()).isEqualTo("PUBLISHED");
    verify(mockRepository).save(argThat(p -> p.getStatus() == PostStatus.PUBLISHED));
}
```

**Run test → FAILS** ✗

**Step 4: Implement use case (GREEN)**
```java
@Service
public class PublishPostService implements PublishPostUseCase {
    private final PostRepository postRepository;

    public PublishPostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    @Transactional
    public Response execute(Command command) {
        var postId = PostId.from(command.postId());
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new PostNotFoundException(postId));

        var published = post.publish();
        var saved = postRepository.save(published);

        return new Response(
            saved.getEntityId().value().toString(),
            saved.getStatus().name()
        );
    }
}
```

**Run test → PASSES** ✓

**Step 5: Refactor (keep tests green)**
- Extract validation logic
- Rename variables for clarity
- Add logging

**Run all tests after each refactoring** ✓

**Step 6: Write controller test (RED)**
```java
@Test
void shouldPublishPostAndReturn200() throws Exception {
    // Test implementation
}
```

**Repeat cycle...**

## Running Tests

### Run all tests
```bash
./gradlew test
```

### Run specific module tests
```bash
./gradlew :modules:content:domain:test
./gradlew :modules:content:application:test
./gradlew :modules:content:adapter:in:web:test
```

### Run specific test class
```bash
./gradlew test --tests CreatePostServiceTest
```

### Run with coverage
```bash
./gradlew test jacocoTestReport
# Open: build/reports/jacoco/test/html/index.html
```

### Run full quality checks
```bash
./gradlew check  # tests + checkstyle + coverage verification
```

## Best Practices

1. **Write the test name first** - It clarifies what you're testing
2. **One assertion per test** - Prefer focused tests over comprehensive ones
3. **Test behavior, not implementation** - Don't test private methods directly
4. **Use meaningful test data** - "John Doe" > "test", "user@example.com" > "a@b.c"
5. **Keep tests fast** - Use mocks for external dependencies
6. **Clean up after tests** - Use `@AfterEach` or `@AfterAll` hooks
7. **Run tests frequently** - After every small change
8. **Commit only when green** - Never commit failing tests
9. **Use AssertJ for fluent assertions** - More readable than JUnit assertions
10. **Use `@DisplayName` for clarity** - Make test output human-readable

## Common Patterns

### Testing Exceptions
```java
@Test
void shouldThrowPostNotFoundExceptionWhenPostNotFound() {
    when(mockRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.execute(command))
        .isInstanceOf(PostNotFoundException.class)
        .hasMessageContaining("Post not found");
}
```

### Testing Events
```java
@Test
void shouldRegisterPostCreatedEventWhenPostIsCreated() {
    var post = Post.create(title, content, authorId);

    var events = post.getEvents();
    assertThat(events).hasSize(1);

    var event = (PostCreatedEvent) events.get(0);
    assertThat(event.postId()).isEqualTo(post.getEntityId());
}
```

### Testing with Multiple Mocks
```java
@Test
void shouldCreatePostAndSendNotification() {
    when(mockPostRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    doNothing().when(mockNotificationService).send(any());

    service.execute(command);

    verify(mockPostRepository).save(any());
    verify(mockNotificationService).send(any());
}
```

### Parameterized Tests
```java
@ParameterizedTest
@ValueSource(strings = {"", " ", "  "})
void shouldRejectBlankTitle(String invalidTitle) {
    assertThatThrownBy(() -> new Title(invalidTitle))
        .isInstanceOf(IllegalArgumentException.class);
}

@ParameterizedTest
@CsvSource({
    "test@example.com, true",
    "invalid-email, false",
    "@example.com, false"
})
void shouldValidateEmailFormat(String email, boolean expected) {
    if (expected) {
        assertThatNoException().isThrownBy(() -> new Email(email));
    } else {
        assertThatThrownBy(() -> new Email(email))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

### Test Fixtures with @BeforeEach
```java
class PostServiceTest {
    private PostRepository mockRepository;
    private PostService service;
    private Post samplePost;

    @BeforeEach
    void setUp() {
        mockRepository = mock(PostRepository.class);
        service = new PostService(mockRepository);
        samplePost = Post.create(
            new Title("Test"),
            new Content("Test"),
            MemberId.generate()
        );
    }

    @Test
    void shouldUseSamplePost() {
        // samplePost is available in all tests
    }
}
```

## AssertJ Assertions Cheat Sheet

```java
// Basic assertions
assertThat(actual).isEqualTo(expected);
assertThat(actual).isNotNull();
assertThat(actual).isNull();
assertThat(actual).isTrue();
assertThat(actual).isFalse();

// String assertions
assertThat(string).isNotBlank();
assertThat(string).startsWith("prefix");
assertThat(string).contains("substring");
assertThat(string).matches("regex");

// Collection assertions
assertThat(list).hasSize(3);
assertThat(list).isEmpty();
assertThat(list).contains(element);
assertThat(list).containsExactly(e1, e2, e3);
assertThat(list).allMatch(predicate);

// Exception assertions
assertThatThrownBy(() -> method())
    .isInstanceOf(CustomException.class)
    .hasMessage("exact message")
    .hasMessageContaining("partial");

assertThatNoException().isThrownBy(() -> method());

// Optional assertions
assertThat(optional).isPresent();
assertThat(optional).isEmpty();
assertThat(optional).hasValue(expected);

// Object field assertions
assertThat(object)
    .extracting("field1", "field2")
    .containsExactly(value1, value2);
```
