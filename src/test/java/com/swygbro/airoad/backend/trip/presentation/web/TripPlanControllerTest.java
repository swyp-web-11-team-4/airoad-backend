package com.swygbro.airoad.backend.trip.presentation.web;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swygbro.airoad.backend.auth.domain.dto.UserPrincipal;
import com.swygbro.airoad.backend.common.domain.dto.CursorPageResponse;
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.presentation.GlobalExceptionHandler;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.member.exception.MemberErrorCode;
import com.swygbro.airoad.backend.trip.application.DailyPlanCommandUseCase;
import com.swygbro.airoad.backend.trip.application.TripPlanUseCase;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanUpdateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.ChannelIdResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.DailyPlanResponse;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanResponse;
import com.swygbro.airoad.backend.trip.exception.TripErrorCode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("TripPlanController 테스트")
class TripPlanControllerTest {

  @Mock private TripPlanUseCase tripPlanUseCase;
  @Mock private DailyPlanCommandUseCase dailyPlanUseCase;
  @InjectMocks private TripPlanController tripPlanController;

  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  private static final String TEST_EMAIL = "test@naver.com";
  private static final Long TEST_CHAT_ROOM_ID = 1L;
  private static final Long TEST_TRIP_PLAN_ID = 100L;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules(); // LocalDate 등을 처리하기 위해 필요

    mockMvc =
        MockMvcBuilders.standaloneSetup(tripPlanController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
    mockUserPrincipal();
  }

  private void mockUserPrincipal() {
    Member member = MemberFixture.withId(1L, MemberFixture.create());
    UserPrincipal userPrincipal = new UserPrincipal(member);
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            userPrincipal, null, userPrincipal.getAuthorities()));
    SecurityContextHolder.setContext(context);
  }

  // @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setUpSecurityContext() {
    Member testMember =
        Member.builder()
            .email(TEST_EMAIL)
            .name("테스트 사용자")
            .imageUrl("https://example.com/profile.jpg")
            .provider(ProviderType.GOOGLE)
            .role(MemberRole.MEMBER)
            .build();

    UserPrincipal userPrincipal = new UserPrincipal(testMember);
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            userPrincipal, null, userPrincipal.getAuthorities());

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
  }

  @Nested
  @DisplayName("generateTripPlan 메서드는")
  class GenerateTripPlan {

    @Test
    @DisplayName("given 유효한 요청 when 여행 계획 생성 세션 생성 then 202 상태와 채널 ID를 반환한다")
    void generateTripPlanSuccess() throws Exception {
      // given
      setUpSecurityContext();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(List.of(PlaceThemeType.HEALING, PlaceThemeType.RESTAURANT))
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(3)
              .region("서울")
              .peopleCount(2)
              .build();

      ChannelIdResponse response = new ChannelIdResponse(TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID);

      given(tripPlanUseCase.createTripPlanSession(eq(TEST_EMAIL), any(TripPlanCreateRequest.class)))
          .willReturn(response);

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isAccepted())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(202))
          .andExpect(jsonPath("$.data.conversationId").value(TEST_CHAT_ROOM_ID))
          .andExpect(jsonPath("$.data.tripPlanId").value(TEST_TRIP_PLAN_ID));

      verify(tripPlanUseCase)
          .createTripPlanSession(eq(TEST_EMAIL), any(TripPlanCreateRequest.class));
    }

    @Test
    @DisplayName("given 테마가 비어있는 요청 when 여행 계획 생성 세션 생성 then 400 에러가 발생한다")
    void generateTripPlanWithEmptyThemes() throws Exception {
      // given
      setUpSecurityContext();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(Collections.emptyList())
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(3)
              .region("서울")
              .peopleCount(2)
              .build();

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(400));

      verify(tripPlanUseCase, times(0))
          .createTripPlanSession(anyString(), any(TripPlanCreateRequest.class));
    }

    @Test
    @DisplayName("given startDate가 null인 요청 when 여행 계획 생성 세션 생성 then 400 에러가 발생한다")
    void generateTripPlanWithNullStartDate() throws Exception {
      // given
      setUpSecurityContext();

      String invalidRequestJson =
          """
          {
            "themes": ["HEALING", "RESTAURANT"],
            "startDate": null,
            "duration": 3,
            "region": "서울",
            "peopleCount": 2
          }
          """;

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(invalidRequestJson))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(400));

      verify(tripPlanUseCase, times(0))
          .createTripPlanSession(anyString(), any(TripPlanCreateRequest.class));
    }

    @Test
    @DisplayName("given duration이 0인 요청 when 여행 계획 생성 세션 생성 then 400 에러가 발생한다")
    void generateTripPlanWithZeroDuration() throws Exception {
      // given
      setUpSecurityContext();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(List.of(PlaceThemeType.HEALING))
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(0)
              .region("서울")
              .peopleCount(2)
              .build();

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(400));

      verify(tripPlanUseCase, times(0))
          .createTripPlanSession(anyString(), any(TripPlanCreateRequest.class));
    }

    @Test
    @DisplayName("given region이 빈 문자열인 요청 when 여행 계획 생성 세션 생성 then 400 에러가 발생한다")
    void generateTripPlanWithBlankRegion() throws Exception {
      // given
      setUpSecurityContext();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(List.of(PlaceThemeType.HEALING))
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(3)
              .region("")
              .peopleCount(2)
              .build();

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(400));

      verify(tripPlanUseCase, times(0))
          .createTripPlanSession(anyString(), any(TripPlanCreateRequest.class));
    }

    @Test
    @DisplayName("given peopleCount가 0인 요청 when 여행 계획 생성 세션 생성 then 400 에러가 발생한다")
    void generateTripPlanWithZeroPeopleCount() throws Exception {
      // given
      setUpSecurityContext();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(List.of(PlaceThemeType.HEALING))
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(3)
              .region("서울")
              .peopleCount(0)
              .build();

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(400));

      verify(tripPlanUseCase, times(0))
          .createTripPlanSession(anyString(), any(TripPlanCreateRequest.class));
    }

    @Test
    @DisplayName("given 존재하지 않는 사용자 when 여행 계획 생성 세션 생성 then 404 에러가 발생한다")
    void generateTripPlanWithNonExistentMember() throws Exception {
      // given
      setUpSecurityContext();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(List.of(PlaceThemeType.HEALING))
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(3)
              .region("서울")
              .peopleCount(2)
              .build();

      given(tripPlanUseCase.createTripPlanSession(eq(TEST_EMAIL), any(TripPlanCreateRequest.class)))
          .willThrow(new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(404));

      verify(tripPlanUseCase)
          .createTripPlanSession(eq(TEST_EMAIL), any(TripPlanCreateRequest.class));
    }

    @Test
    @DisplayName("given 여러 테마를 포함한 요청 when 여행 계획 생성 세션 생성 then 성공적으로 처리된다")
    void generateTripPlanWithMultipleThemes() throws Exception {
      // given
      setUpSecurityContext();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .themes(
                  List.of(
                      PlaceThemeType.HEALING,
                      PlaceThemeType.RESTAURANT,
                      PlaceThemeType.EXPERIENCE_ACTIVITY))
              .startDate(LocalDate.of(2025, 12, 1))
              .duration(5)
              .region("제주")
              .peopleCount(4)
              .build();

      ChannelIdResponse response = new ChannelIdResponse(TEST_CHAT_ROOM_ID, TEST_TRIP_PLAN_ID);

      given(tripPlanUseCase.createTripPlanSession(eq(TEST_EMAIL), any(TripPlanCreateRequest.class)))
          .willReturn(response);

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isAccepted())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(202));

      verify(tripPlanUseCase)
          .createTripPlanSession(eq(TEST_EMAIL), any(TripPlanCreateRequest.class));
    }
  }

  @Nested
  @DisplayName("startTripPlanGeneration 메서드는")
  class StartTripPlanGeneration {

    @Test
    @DisplayName("given 유효한 채팅방 ID when 여행 계획 생성 시작 then 200 상태와 성공 메시지를 반환한다")
    void startTripPlanGenerationSuccess() throws Exception {
      // given
      setUpSecurityContext();
      doNothing().when(tripPlanUseCase).startTripPlanGeneration(TEST_EMAIL, TEST_CHAT_ROOM_ID);

      // when & then
      mockMvc
          .perform(post("/api/v1/trips/{chatRoomId}", TEST_CHAT_ROOM_ID))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(200))
          .andExpect(jsonPath("$.data.message").value("여행 일정 생성을 시작합니다."));

      verify(tripPlanUseCase).startTripPlanGeneration(TEST_EMAIL, TEST_CHAT_ROOM_ID);
    }

    @Test
    @DisplayName("given 존재하지 않는 채팅방 ID when 여행 계획 생성 시작 then 예외가 발생한다")
    void startTripPlanGenerationWithNonExistentChatRoom() throws Exception {
      // given
      setUpSecurityContext();
      Long nonExistentChatRoomId = 999L;

      doThrow(new RuntimeException("채팅방을 찾을 수 없습니다."))
          .when(tripPlanUseCase)
          .startTripPlanGeneration(TEST_EMAIL, nonExistentChatRoomId);

      // when & then
      mockMvc
          .perform(post("/api/v1/trips/{chatRoomId}", nonExistentChatRoomId))
          .andDo(print())
          .andExpect(status().is5xxServerError());

      verify(tripPlanUseCase).startTripPlanGeneration(TEST_EMAIL, nonExistentChatRoomId);
    }

    @Test
    @DisplayName("given 다양한 채팅방 ID when 여행 계획 생성 시작 then 각각의 ID로 서비스가 호출된다")
    void startTripPlanGenerationWithDifferentChatRoomIds() throws Exception {
      // given
      setUpSecurityContext();
      Long[] chatRoomIds = {1L, 100L, 9999L};

      for (Long chatRoomId : chatRoomIds) {
        doNothing().when(tripPlanUseCase).startTripPlanGeneration(TEST_EMAIL, chatRoomId);

        // when & then
        mockMvc
            .perform(post("/api/v1/trips/{chatRoomId}", chatRoomId))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.message").value("여행 일정 생성을 시작합니다."));

        verify(tripPlanUseCase).startTripPlanGeneration(TEST_EMAIL, chatRoomId);
      }
    }
  }

  @Nested
  @DisplayName("사용자 여행 일정 목록 조회 API 호출 시")
  class GetUserTripPlans {

    @Test
    @DisplayName("인증된 사용자가 자신의 여행 목록을 성공적으로 조회한다")
    void getsUserTripPlansSuccessfully() throws Exception {
      // given
      CursorPageResponse<TripPlanResponse> response =
          CursorPageResponse.of(Collections.emptyList(), null, false);
      given(tripPlanUseCase.getUserTripPlans(anyLong(), anyInt(), any(), anyString()))
          .willReturn(response);

      // when & then
      mockMvc
          .perform(get("/api/v1/trips"))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.hasNext").value(false));
    }
  }

  @Nested
  @DisplayName("여행 일정 삭제 API 호출 시")
  class DeleteTripPlan {

    @Test
    @DisplayName("인증된 사용자가 자신의 여행 일정을 성공적으로 삭제한다")
    void deletesOwnTripPlanSuccessfully() throws Exception {
      // given
      Long tripPlanId = 1L;
      willDoNothing().given(tripPlanUseCase).deleteTripPlan(anyLong(), anyLong());

      // when & then
      mockMvc
          .perform(delete("/api/v1/trips/{tripPlanId}", tripPlanId))
          .andDo(print())
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("여행 일정 수정 API 호출 시")
  class UpdateTripPlan {

    @Test
    @DisplayName("인증된 사용자가 자신의 여행 일정을 성공적으로 수정한다")
    void updatesTripPlanSuccessfully() throws Exception {
      // given
      mockUserPrincipal();
      Long tripPlanId = 1L;
      TripPlanUpdateRequest request = new TripPlanUpdateRequest("새로운 여행 제목");

      willDoNothing()
          .given(tripPlanUseCase)
          .updateTripPlan(anyLong(), anyLong(), any(TripPlanUpdateRequest.class));

      // when & then
      mockMvc
          .perform(
              patch("/api/v1/trips/{tripPlanId}", tripPlanId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("요청 본문이 유효하지 않으면 400 Bad Request를 반환한다")
    void failsWithInvalidRequest() throws Exception {
      // given
      mockUserPrincipal();
      Long tripPlanId = 1L;
      TripPlanUpdateRequest request = new TripPlanUpdateRequest(" ");

      // when & then
      mockMvc
          .perform(
              patch("/api/v1/trips/{tripPlanId}", tripPlanId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.data.errors[0].field").value("title"))
          .andExpect(jsonPath("$.data.errors[0].message").value("여행 제목은 비워둘 수 없습니다."));
    }
  }

  @Nested
  @DisplayName("getDailyPlans 메서드는")
  class GetDailyPlans {

    @Test
    @DisplayName("유효한 tripPlanId로 일차별 일정 목록을 조회한다")
    void shouldReturnDailyPlans() throws Exception {
      // given
      mockUserPrincipal();
      Long tripPlanId = 1L;

      DailyPlanResponse dailyPlan1 =
          DailyPlanResponse.builder()
              .id(1L)
              .dayNumber(1)
              .date("2025-12-01")
              .title("1일차 여행")
              .description("AI가 생성한 1일차 여행 일정입니다.")
              .scheduledPlaces(Collections.emptyList())
              .build();

      DailyPlanResponse dailyPlan2 =
          DailyPlanResponse.builder()
              .id(2L)
              .dayNumber(2)
              .date("2025-12-02")
              .title("2일차 여행")
              .description("AI가 생성한 2일차 여행 일정입니다.")
              .scheduledPlaces(Collections.emptyList())
              .build();

      List<DailyPlanResponse> dailyPlans = List.of(dailyPlan1, dailyPlan2);

      given(dailyPlanUseCase.getDailyPlanListByTripPlanId(eq(tripPlanId), anyLong()))
          .willReturn(dailyPlans);

      // when & then
      mockMvc
          .perform(get("/api/v1/trips/daily-plans/{tripPlanId}", tripPlanId))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(200))
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data.length()").value(2))
          .andExpect(jsonPath("$.data[0].dayNumber").value(1))
          .andExpect(jsonPath("$.data[0].date").value("2025-12-01"))
          .andExpect(jsonPath("$.data[1].dayNumber").value(2))
          .andExpect(jsonPath("$.data[1].date").value("2025-12-02"));

      verify(dailyPlanUseCase).getDailyPlanListByTripPlanId(eq(tripPlanId), anyLong());
    }

    @Test
    @DisplayName("DailyPlan이 없는 TripPlan을 조회하면 빈 배열을 반환한다")
    void shouldReturnEmptyArrayWhenNoDailyPlans() throws Exception {
      // given
      mockUserPrincipal();
      Long tripPlanId = 1L;

      given(dailyPlanUseCase.getDailyPlanListByTripPlanId(eq(tripPlanId), anyLong()))
          .willReturn(Collections.emptyList());

      // when & then
      mockMvc
          .perform(get("/api/v1/trips/daily-plans/{tripPlanId}", tripPlanId))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(200))
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data.length()").value(0));

      verify(dailyPlanUseCase).getDailyPlanListByTripPlanId(eq(tripPlanId), anyLong());
    }

    @Test
    @DisplayName("존재하지 않는 TripPlan ID로 조회하면 404 에러를 반환한다")
    void shouldReturn404WhenTripPlanNotFound() throws Exception {
      // given
      mockUserPrincipal();
      Long nonExistentTripPlanId = 999L;

      given(dailyPlanUseCase.getDailyPlanListByTripPlanId(eq(nonExistentTripPlanId), anyLong()))
          .willThrow(new BusinessException(TripErrorCode.TRIP_PLAN_NOT_FOUND));

      // when & then
      mockMvc
          .perform(get("/api/v1/trips/daily-plans/{tripPlanId}", nonExistentTripPlanId))
          .andDo(print())
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.data.code").value("TRIP101"));

      verify(dailyPlanUseCase).getDailyPlanListByTripPlanId(eq(nonExistentTripPlanId), anyLong());
    }

    @Test
    @DisplayName("다른 사용자의 TripPlan을 조회하면 403 에러를 반환한다")
    void shouldReturn403WhenForbidden() throws Exception {
      // given
      mockUserPrincipal();
      Long tripPlanId = 1L;

      given(dailyPlanUseCase.getDailyPlanListByTripPlanId(eq(tripPlanId), anyLong()))
          .willThrow(new BusinessException(TripErrorCode.TRIP_PLAN_FORBIDDEN));

      // when & then
      mockMvc
          .perform(get("/api/v1/trips/daily-plans/{tripPlanId}", tripPlanId))
          .andDo(print())
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(403))
          .andExpect(jsonPath("$.data.code").value("TRIP102"));

      verify(dailyPlanUseCase).getDailyPlanListByTripPlanId(eq(tripPlanId), anyLong());
    }

    @Test
    @DisplayName("여러 일차의 DailyPlan을 순서대로 조회한다")
    void shouldReturnMultipleDailyPlansInOrder() throws Exception {
      // given
      mockUserPrincipal();
      Long tripPlanId = 1L;

      List<DailyPlanResponse> dailyPlans =
          List.of(
              DailyPlanResponse.builder()
                  .id(1L)
                  .dayNumber(1)
                  .date("2025-12-01")
                  .title("1일차 여행")
                  .description("AI가 생성한 1일차 여행 일정입니다.")
                  .scheduledPlaces(Collections.emptyList())
                  .build(),
              DailyPlanResponse.builder()
                  .id(2L)
                  .dayNumber(2)
                  .date("2025-12-02")
                  .title("2일차 여행")
                  .description("AI가 생성한 2일차 여행 일정입니다.")
                  .scheduledPlaces(Collections.emptyList())
                  .build(),
              DailyPlanResponse.builder()
                  .id(3L)
                  .dayNumber(3)
                  .date("2025-12-03")
                  .title("3일차 여행")
                  .description("AI가 생성한 3일차 여행 일정입니다.")
                  .scheduledPlaces(Collections.emptyList())
                  .build());

      given(dailyPlanUseCase.getDailyPlanListByTripPlanId(eq(tripPlanId), anyLong()))
          .willReturn(dailyPlans);

      // when & then
      mockMvc
          .perform(get("/api/v1/trips/daily-plans/{tripPlanId}", tripPlanId))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.length()").value(3))
          .andExpect(jsonPath("$.data[0].dayNumber").value(1))
          .andExpect(jsonPath("$.data[1].dayNumber").value(2))
          .andExpect(jsonPath("$.data[2].dayNumber").value(3));

      verify(dailyPlanUseCase).getDailyPlanListByTripPlanId(eq(tripPlanId), anyLong());
    }
  }
}
