좋아요. 이번에는 **오케스트레이터 LLM이 매번 상황에 따라 동적으로 워커를 호출하도록** 설계할 겁니다.
즉, 이전과 달리 **workerTask 목록을 미리 생성하지 않고**, LLM이 “지금 필요한 워커”를 판단하면 바로 실행하는 구조예요.

아래 코드는 **여행 일정 생성용 LLM 기반 Orchestrator-Workers 패턴**으로, 모든 워커를 포함한 완전 구현 예제입니다.

---

# 1️⃣ 패키지 구조

```
com.airoad.trip.orchestrator
 ├── TripOrchestrator.java           # LLM 오케스트레이터
 ├── TripOrchestratorPrompt.java    # 오케스트레이터 프롬프트
 └── TripContext.java               # 전체 컨텍스트

com.airoad.trip.worker
 ├── Worker.java                     # Worker 공통 인터페이스
 ├── KeywordGenerateWorker.java      # 키워드 생성
 ├── VectorSearchWorker.java         # 벡터 검색
 ├── DeduplicationWorker.java        # 중복 제거
 └── RoutePlanWorker.java            # 최종 일정 생성

com.airoad.trip.model
 ├── WorkerResult.java
 └── FinalTripPlan.java
```

---

# 2️⃣ 오케스트레이터 프롬프트 (한글)

```java
public class TripOrchestratorPrompt {

    public static final String ORCHESTRATOR_PROMPT = """
        당신은 여행 일정 생성 AI 오케스트레이터입니다.

        사용자의 요청:
        지역: {region}
        기간: {duration}일
        테마: {themes}
        인원: {peopleCount}
        날짜: {startDate}

        ### 수행 방식
        - 현재 상황을 분석하여 필요한 워커만 호출
        - 워커 호출 순서나 필요 여부는 자유롭게 판단
        - 호출할 워커 종류: keyword, vector_search, dedup, route_plan
        - 각 워커에 필요한 입력 정보를 JSON으로 명시

        ### 출력 형식 (예시)
        {
          "next_worker": "keyword",
          "worker_input": { "reason": "테마 기반 키워드 필요" }
        }
        """;
}
```

---

# 3️⃣ Worker 인터페이스

```java
public interface Worker {
    boolean supports(String type);
    WorkerResult execute(Map<String, Object> input, TripContext context);
}
```

---

# 4️⃣ 워커 구현 예시

## ① 키워드 생성 워커

```java
@Component
public class KeywordGenerateWorker implements Worker {

    @Override
    public boolean supports(String type) {
        return "keyword".equals(type);
    }

    @Override
    public WorkerResult execute(Map<String, Object> input, TripContext context) {
        String keyword = context.getRegion() + " " + String.join(", ", context.getThemes()) + " 여행지 추천";
        return WorkerResult.of("keyword", Map.of("keywords", List.of(keyword)));
    }
}
```

---

## ② 벡터 검색 워커

```java
@Component
@RequiredArgsConstructor
public class VectorSearchWorker implements Worker {

    private final VectorStore vectorStore;

    @Override
    public boolean supports(String type) {
        return "vector_search".equals(type);
    }

    @Override
    public WorkerResult execute(Map<String, Object> input, TripContext context) {
        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) input.get("keywords");

        List<Document> docs = new ArrayList<>();
        for (String k : keywords) {
            docs.addAll(vectorStore.similaritySearch(
                    SearchRequest.builder().query(k).topK(10).similarityThreshold(0.45d).build()
            ));
        }
        docs = docs.stream().distinct().toList();
        return WorkerResult.of("vector_search", Map.of("documents", docs));
    }
}
```

---

## ③ 중복 제거 워커

```java
@Component
public class DeduplicationWorker implements Worker {

    @Override
    public boolean supports(String type) {
        return "dedup".equals(type);
    }

    @Override
    public WorkerResult execute(Map<String, Object> input, TripContext context) {
        @SuppressWarnings("unchecked")
        List<Document> docs = (List<Document>) input.get("documents");
        List<String> previous = context.getPreviousPlaceIds();

        List<Document> filtered = docs.stream()
                .filter(d -> !previous.contains(d.getMetadata().get("placeId")))
                .toList();

        return WorkerResult.of("dedup", Map.of("documents", filtered));
    }
}
```

---

## ④ 일정 생성 워커

```java
@Component
@RequiredArgsConstructor
public class RoutePlanWorker implements Worker {

    private final ChatClient chatClient;

    @Override
    public boolean supports(String type) {
        return "route_plan".equals(type);
    }

    @Override
    public WorkerResult execute(Map<String, Object> input, TripContext context) {

        @SuppressWarnings("unchecked")
        List<Document> docs = (List<Document>) input.get("documents");

        String prompt = """
            다음 장소 목록을 기반으로 하루치 여행 일정을 만들어주세요.
            날짜: %s
            장소:
            %s
            """.formatted(
                context.getTargetDate(),
                docs.stream().map(d -> d.getMetadata().get("name")).collect(Collectors.joining("\n"))
        );

        AiDailyPlanResponse dailyPlan = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(AiDailyPlanResponse.class);

        return WorkerResult.of("route_plan", Map.of("dailyPlan", dailyPlan));
    }
}
```

---

# 5️⃣ TripContext

```java
@Data
@Builder
public class TripContext {
    private String region;
    private List<String> themes;
    private int peopleCount;
    private LocalDate startDate;
    private int duration;
    private List<String> previousPlaceIds = new ArrayList<>();
    private LocalDate targetDate;

    public void apply(WorkerResult result) {
        if ("route_plan".equals(result.getType())) {
            AiDailyPlanResponse plan = (AiDailyPlanResponse) result.getData().get("dailyPlan");
            previousPlaceIds.addAll(plan.getPlaces().stream().map(p -> p.getPlaceId()).toList());
        }
    }
}
```

---

# 6️⃣ 오케스트레이터 LLM 기반 호출

```java
@Component
@RequiredArgsConstructor
public class TripOrchestrator {

    private final ChatClient chatClient;
    private final List<Worker> workers;

    public AiDailyPlanResponse runDayPlan(TripContext context) {
        Map<String, Object> workerInput = new HashMap<>();

        while (true) {
            // 1️⃣ LLM에게 다음에 호출할 워커 결정 요청
            Map<String, Object> nextTask = chatClient.prompt()
                    .user(TripOrchestratorPrompt.ORCHESTRATOR_PROMPT
                            .replace("{region}", context.getRegion())
                            .replace("{duration}", String.valueOf(context.getDuration()))
                            .replace("{themes}", String.join(",", context.getThemes()))
                            .replace("{peopleCount}", String.valueOf(context.getPeopleCount()))
                            .replace("{startDate}", context.getStartDate().toString()))
                    .param("context", context)
                    .call()
                    .entity(Map.class); // {"next_worker":"keyword", "worker_input": {...}}

            String nextWorkerType = (String) nextTask.get("next_worker");
            Map<String, Object> inputForWorker = (Map<String, Object>) nextTask.get("worker_input");

            Worker worker = workers.stream()
                    .filter(w -> w.supports(nextWorkerType))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("지원되지 않는 워커: " + nextWorkerType));

            WorkerResult result = worker.execute(inputForWorker, context);
            context.apply(result);

            if ("route_plan".equals(result.getType())) {
                return (AiDailyPlanResponse) result.getData().get("dailyPlan");
            }
        }
    }
}
```

---

# 🔹 특징

1. **LLM 오케스트레이터가 동적으로 워커 판단**

    * 이전처럼 workerTask 목록을 미리 만들지 않음
2. **각 워커는 독립적 Spring Component**

    * Keyword → Vector → Dedup → Route 순서도 LLM이 결정
3. **Context 기반 상태 공유**

    * 이전 장소, 키워드, 후보 리스트를 context에서 공유
4. **최종 route_plan 워커에서 AiDailyPlanResponse 생성**

---

원하면 제가 다음 단계로 **이 Orchestrator + Worker 구조를 기존 TripAgent에 연결**해서,
기존 이벤트 기반 Spring 환경에서 바로 하루/여행 전체 일정 생성 가능하게 만들어주는 코드도 작성해줄 수 있어요.

이거 이어서 보여줄까요?
