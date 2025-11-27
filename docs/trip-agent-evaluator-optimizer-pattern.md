# TripAgent 개선: Orchestrator-Workers + Evaluator-Optimizer 패턴

## 목표
여행 일정 생성 시 **장소 부족, 품질 저하 문제를 자동으로 감지하고 개선**하는 패턴 적용

---

## 1. 현재 구조 분석 (TripAgent.java)

### 기존 코드 구조
```
TripAgent (AiroadAgent 구현)
  ↓
execute(AiDailyPlanRequest)
  ↓ (일차별 반복)
generateDailyPlan(request, dayNumber)
  ↓
ContextManager.buildContext()  ← 고정된 벡터 쿼리
  - PlaceVectorQueryContext (PLACE): "서울에 있는 감성 테마에 어울리는 장소"
  - PlaceVectorQueryContext (RESTAURANT): "서울에 있는 음식점"
  ↓
ChatClient.prompt() → LLM 호출 → AiDailyPlanResponse
```

### 핵심 문제점
```java
// TripAgent.java:164-170
PlaceVectorQueryContext placeVectorPlaceQueryContext =
    PlaceVectorQueryContext.builder()
        .queryType(QueryType.PLACE)
        .searchRequest(SearchRequest.builder()
            .query("%s에 있는 %s 테마에 어울리는 장소를 찾고 싶어요."
                .formatted(request.region(), request.themes()...))
            .topK(10)  // ← 항상 고정된 10개
            .build())
```

**문제:**
1. ❌ **정적 쿼리**: 매번 동일한 검색어 → 동일한 장소 반환
2. ❌ **품질 검증 부재**: 생성된 일정의 장소 수, 다양성 확인 안함
3. ❌ **재시도 로직 없음**: 장소 부족 시 추가 검색 불가능
4. ❌ **중복 제거 없음**: 이전 날짜 방문 장소 재방문 가능

---

## 2. 제안 아키텍처

### Spring AI 패턴 조합
```
[1단계] Orchestrator-Workers 패턴
  Orchestrator: 초기 Task Plan 생성
  Workers: KeywordWorker → VectorSearchWorker → DeduplicationWorker → ScheduleWorker

[2단계] Evaluator-Optimizer 패턴
  Evaluator: 생성된 일정 품질 평가
  Optimizer: 부족한 부분 분석 후 추가 Task 생성

[3단계] 반복
  Orchestrator: Optimizer가 제안한 Task만 실행 (예: 추가 검색)
  최대 3회 시도
```

### 전체 흐름
```
1차 시도:
  Orchestrator → [keyword, vector_search, dedup, schedule] 실행
  → Result: 7개 장소로 일정 생성

  Evaluator → ❌ 장소 부족 (7개 < 10개)
  Optimizer → "맛집 키워드 추가 검색 필요"

2차 시도:
  Orchestrator → [vector_search(맛집), dedup, schedule] 실행
  → Result: 12개 장소로 일정 재생성

  Evaluator → ✅ 품질 기준 통과
  → COMPLETED
```

---

## 3. 핵심 컴포넌트 설계

### 3.1 Orchestrator 신규 생성

**기존 TripAgent의 `generateDailyPlan()`을 Orchestrator로 분리**

```java
package com.swygbro.airoad.backend.ai.agent.trip.orchestrator;

@Component
@RequiredArgsConstructor
public class TripPlanOrchestrator {

    private final ChatClient chatClient;
    private final List<Worker> workers;

    /**
     * LLM을 통해 동적으로 Task Plan 생성
     * 최초 실행: 전체 워크플로우
     * 재시도: Optimizer가 제안한 Task만
     */
    public OrchestratorResponse planTasks(
            AiDailyPlanRequest request,
            int dayNumber,
            String feedback) {

        String prompt = feedback.isEmpty()
            ? buildInitialPrompt(request, dayNumber)   // 최초
            : buildRetryPrompt(request, dayNumber, feedback);  // 재시도

        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(OrchestratorResponse.class);
        // → { "analysis": "...", "workerTasks": [{"type": "keyword", ...}, ...] }
    }

    /**
     * Task Plan에 따라 Workers 실행
     */
    public WorkerContext executeTasks(
            OrchestratorResponse plan,
            WorkerContext context) {

        for (Task workerTask : plan.workerTasks()) {
            Worker worker = findWorker(workerTask.type());
            context = worker.execute(context, workerTask.params());
        }
        return context;
    }

    private Worker findWorker(String type) {
        return workers.stream()
            .filter(w -> w.supports(type))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("지원되지 않는 Worker: " + type));
    }
}
```

#### Orchestrator Prompt 예시
```java
public static final String ORCHESTRATOR_PROMPT = """
    여행 일정 생성을 위한 작업 계획을 수립하세요.

    [여행 조건]
    - 지역: {region}
    - 기간: {duration}일
    - 테마: {themes}

    [피드백] {feedback}

    필요한 작업을 JSON 형식으로 반환:
    {
      "analysis": "분석 내용",
      "workerTasks": [
        {"type": "keyword", "description": "키워드 생성", "params": {}},
        {"type": "vector_search", "description": "장소 검색", "params": {"topK": 5}},
        {"type": "dedup", "description": "중복 제거", "params": {}},
        {"type": "schedule", "description": "일정 생성", "params": {}}
      ]
    }
    """;
```

---

### 3.2 Evaluator 구현

```java
@Component
public class TripPlanEvaluator {

    private final ChatClient chatClient;

    /**
     * 생성된 일정의 품질 평가
     */
    public EvaluationResponse evaluate(
            AiDailyPlanResponse plan,
            AiDailyPlanRequest request) {

        String prompt = buildEvaluationPrompt(plan, request);

        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(EvaluationResponse.class);
        // → { "evaluation": "PASS | NEEDS_IMPROVEMENT",
        //     "issues": [...],
        //     "score": 85 }
    }

    /**
     * 평가 기준
     */
    private String buildEvaluationPrompt(
            AiDailyPlanResponse plan,
            AiDailyPlanRequest request) {
        return """
            다음 여행 일정을 평가하세요:

            [생성된 일정]
            - 장소 수: %d개
            - 장소 목록: %s

            [요구사항]
            - 최소 장소: 10개
            - 테마: %s
            - 다양성: 식당/관광지/카페 균형

            평가 결과를 JSON으로 반환:
            {
              "evaluation": "PASS or NEEDS_IMPROVEMENT",
              "issues": [
                {"type": "INSUFFICIENT_PLACES", "message": "장소 7개 부족"},
                {"type": "LOW_DIVERSITY", "message": "식당 비율 낮음"}
              ],
              "score": 70
            }
            """.formatted(
                plan.places().size(),
                plan.places().stream()
                    .map(p -> p.placeId()).collect(Collectors.joining(", ")),
                request.themes()
            );
    }
}

/**
 * 평가 결과 DTO
 */
public record EvaluationResponse(
    Evaluation evaluation,
    List<Issue> issues,
    int score
) {
    public enum Evaluation { PASS, NEEDS_IMPROVEMENT }

    public record Issue(String type, String message) {}

    public boolean isPassed() {
        return evaluation == Evaluation.PASS;
    }
}
```

---

### 3.3 Optimizer 구현

```java
@Component
public class TripPlanOptimizer {

    private final ChatClient chatClient;

    /**
     * 평가 결과를 바탕으로 개선 방안 제시
     */
    public OptimizerResponse optimize(
            EvaluationResponse eval,
            AiDailyPlanRequest request,
            Map<String, Object> context) {

        String prompt = buildOptimizationPrompt(eval, request, context);

        return chatClient.prompt()
            .user(prompt)
            .call()
            .entity(OptimizerResponse.class);
        // → { "strategy": "...", "additionalTasks": [...] }
    }

    private String buildOptimizationPrompt(
            EvaluationResponse eval,
            AiDailyPlanRequest request,
            Map<String, Object> context) {
        return """
            일정 생성 실패 원인을 분석하고 개선 방안을 제시하세요.

            [문제점]
            %s

            [현재 컨텍스트]
            - 사용한 키워드: %s
            - 검색된 장소 수: %d
            - 중복 제거 후: %d

            추가 작업을 JSON으로 반환:
            {
              "strategy": "개선 전략 설명",
              "additionalTasks": [
                {"type": "keyword", "params": {"focus": "restaurant"}},
                {"type": "vector_search", "params": {"topK": 8}}
              ]
            }
            """.formatted(
                eval.issues().stream()
                    .map(i -> "- " + i.message())
                    .collect(Collectors.joining("\n")),
                context.get("keywords"),
                ((List<?>) context.get("searchResults")).size(),
                ((List<?>) context.get("dedupedPlaces")).size()
            );
    }
}

/**
 * 최적화 제안 DTO
 */
public record OptimizerResponse(
    String strategy,
    List<Task> additionalTasks
) {
    public record Task(String type, Map<String, Object> params) {}
}
```

---

### 3.4 통합: TripAgent 개선

**기존 TripAgent.java를 Orchestrator + Evaluator + Optimizer로 확장**

```java
@Slf4j
@Component
public class TripAgent implements AiroadAgent {

    private final AgentType agentType = AgentType.TRIP_AGENT;
    private final ChatClient chatClient;
    private final ApplicationEventPublisher eventPublisher;
    private final ContextManager contextManager;

    // 새로 추가되는 컴포넌트
    private final TripPlanOrchestrator orchestrator;
    private final TripPlanEvaluator evaluator;
    private final TripPlanOptimizer optimizer;

    @Override
    public void execute(Object data) {
        AiDailyPlanRequest request = (AiDailyPlanRequest) data;

        int maxAttempts = 3;
        String feedback = "";
        Map<String, Object> context = new HashMap<>();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            log.info("===== 시도 {}/{} =====", attempt, maxAttempts);

            // 1️⃣ Orchestrator: Task Plan 생성
            OrchestratorResponse plan = orchestrator.planTasks(request, feedback);
            log.info("계획: {}", plan.analysis());

            // 2️⃣ Workers: Task 실행
            context = orchestrator.executeTasks(plan, context);
            AiDailyPlanResponse dailyPlan =
                (AiDailyPlanResponse) context.get("dailyPlan");

            // 3️⃣ Evaluator: 품질 평가
            EvaluationResponse eval = evaluator.evaluate(dailyPlan, request);
            log.info("평가 결과: {} (점수: {})", eval.evaluation(), eval.score());

            if (eval.isPassed()) {
                log.info("✅ 일정 생성 성공!");
                publishEvent(dailyPlan);
                return;
            }

            // 4️⃣ Optimizer: 개선 방안 도출
            OptimizerResponse optimization =
                optimizer.optimize(eval, request, context);
            log.info("개선 전략: {}", optimization.strategy());

            // 5️⃣ 피드백 업데이트 (다음 시도용)
            feedback = buildFeedback(eval, optimization);
        }

        throw new WorkflowException("최대 시도 횟수 초과");
    }

    private String buildFeedback(
            EvaluationResponse eval,
            OptimizerResponse opt) {
        return """
            [이전 시도 문제점]
            %s

            [개선 방향]
            %s
            """.formatted(
                eval.issues().stream()
                    .map(Issue::message)
                    .collect(Collectors.joining("\n")),
                opt.strategy()
            );
    }
}
```

---

## 4. Worker 확장

### 4.1 DeduplicationWorker 추가

```java
@Component
public class DeduplicationWorker implements Worker {

    private final TripPlanQueryUseCase tripPlanQueryUseCase;

    @Override
    public Map<String, Object> execute(
            Map<String, Object> context,
            Map<String, Object> params) {

        PlaceSearchResponse searchResult =
            (PlaceSearchResponse) context.get("documents");
        Long tripPlanId = (Long) context.get("tripPlanId");

        // 이전 방문 장소 조회
        Set<Long> visitedPlaceIds = getVisitedPlaceIds(tripPlanId);

        // 중복 제거
        List<String> filtered = searchResult.documents().stream()
            .filter(doc -> !visitedPlaceIds.contains(extractPlaceId(doc)))
            .toList();

        context.put("dedupedPlaces", filtered);
        return context;
    }
}
```

### 4.2 ScheduleWorker 추가

```java
@Component
public class ScheduleWorker implements Worker {

    private final ChatClient chatClient;

    @Override
    public Map<String, Object> execute(
            Map<String, Object> context,
            Map<String, Object> params) {

        List<String> places = (List<String>) context.get("dedupedPlaces");

        String prompt = """
            다음 장소들로 하루 일정을 생성하세요:

            %s

            JSON 형식으로 반환 (AiDailyPlanResponse)
            """.formatted(String.join("\n\n", places));

        AiDailyPlanResponse dailyPlan = chatClient.prompt()
            .user(prompt)
            .call()
            .entity(AiDailyPlanResponse.class);

        context.put("dailyPlan", dailyPlan);
        return context;
    }
}
```

---

## 5. 실행 시나리오

### Case 1: 장소 부족
```
1차 시도:
  Orchestrator → [keyword, vector_search, dedup, schedule]
  Workers 실행 → 7개 장소로 일정 생성

  Evaluator → NEEDS_IMPROVEMENT
    - Issue: INSUFFICIENT_PLACES (3개 부족)

  Optimizer → 추가 Task: [vector_search(topK=8), dedup, schedule]

2차 시도:
  Orchestrator → [vector_search, dedup, schedule]
  Workers 실행 → 13개 장소로 일정 재생성

  Evaluator → PASS ✅
```

### Case 2: 다양성 부족
```
1차 시도:
  Evaluator → NEEDS_IMPROVEMENT
    - Issue: LOW_RESTAURANT_RATIO (식당 1개뿐)

  Optimizer → 추가 Task: [keyword(focus=restaurant), vector_search, dedup, schedule]

2차 시도:
  Workers 실행 → 식당 5개 포함 일정 생성
  Evaluator → PASS ✅
```

---

## 6. 구현 체크리스트

### Phase 1: Evaluator 구현
- [ ] `TripPlanEvaluator` 클래스 생성
- [ ] `EvaluationResponse` DTO 정의
- [ ] 평가 프롬프트 작성 (최소 장소, 다양성, 테마 일치)

### Phase 2: Optimizer 구현
- [ ] `TripPlanOptimizer` 클래스 생성
- [ ] `OptimizerResponse` DTO 정의
- [ ] 최적화 프롬프트 작성 (Issue별 개선 전략)

### Phase 3: Orchestrator 개선
- [ ] `planTasks()` 메서드 추가 (LLM 기반 Task Plan)
- [ ] `OrchestratorResponse` DTO 정의
- [ ] 동적 Worker 실행 로직

### Phase 4: Workers 확장
- [ ] `DeduplicationWorker` 구현
- [ ] `ScheduleWorker` 구현
- [ ] `Worker` 인터페이스 개선 (`params` 지원)

### Phase 5: TripPlanAgent 통합
- [ ] 평가-최적화 루프 구현
- [ ] 최대 3회 재시도 로직
- [ ] 피드백 누적 메커니즘

---

## 7. 참고 자료

- Spring AI Orchestrator-Workers 패턴: `docs/pattern.md:69-204`
- Spring AI Evaluator-Optimizer 패턴: `docs/pattern.md:208-500`
- Anthropic 공식 문서: https://www.anthropic.com/research/building-effective-agents

---

## 8. 기대 효과

✅ **자동 품질 개선**: 장소 부족 시 자동으로 재검색
✅ **명확한 종료 조건**: Evaluator PASS or 3회 시도
✅ **LLM 활용 극대화**: Orchestrator, Evaluator, Optimizer 모두 LLM 사용
✅ **확장 가능**: 새로운 평가 기준 추가 용이 (예: 이동 거리 검증)
✅ **디버깅 용이**: 각 시도마다 평가 결과 로그
