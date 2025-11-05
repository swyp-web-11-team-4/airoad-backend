package com.swygbro.airoad.backend.trip.presentation.web;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.common.presentation.GlobalExceptionHandler;
import com.swygbro.airoad.backend.content.domain.entity.PlaceThemeType;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.member.exception.MemberErrorCode;
import com.swygbro.airoad.backend.trip.application.TripUseCase;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.ChannelIdResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TripPlanControllerTest {

  @Mock private TripUseCase tripUseCase;

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
        MockMvcBuilders.standaloneSetup(new TripPlanController(tripUseCase))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
  }

  @AfterEach
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

      given(tripUseCase.createTripPlanSession(eq(TEST_EMAIL), any(TripPlanCreateRequest.class)))
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

      verify(tripUseCase).createTripPlanSession(eq(TEST_EMAIL), any(TripPlanCreateRequest.class));
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

      verify(tripUseCase, times(0))
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

      verify(tripUseCase, times(0))
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

      verify(tripUseCase, times(0))
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

      verify(tripUseCase, times(0))
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

      verify(tripUseCase, times(0))
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

      given(tripUseCase.createTripPlanSession(eq(TEST_EMAIL), any(TripPlanCreateRequest.class)))
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

      verify(tripUseCase).createTripPlanSession(eq(TEST_EMAIL), any(TripPlanCreateRequest.class));
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

      given(tripUseCase.createTripPlanSession(eq(TEST_EMAIL), any(TripPlanCreateRequest.class)))
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

      verify(tripUseCase).createTripPlanSession(eq(TEST_EMAIL), any(TripPlanCreateRequest.class));
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
      doNothing().when(tripUseCase).startTripPlanGeneration(TEST_EMAIL, TEST_CHAT_ROOM_ID);

      // when & then
      mockMvc
          .perform(post("/api/v1/trips/{chatRoomId}", TEST_CHAT_ROOM_ID))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(200))
          .andExpect(jsonPath("$.data.message").value("여행 일정 생성을 시작합니다."));

      verify(tripUseCase).startTripPlanGeneration(TEST_EMAIL, TEST_CHAT_ROOM_ID);
    }

    @Test
    @DisplayName("given 존재하지 않는 채팅방 ID when 여행 계획 생성 시작 then 예외가 발생한다")
    void startTripPlanGenerationWithNonExistentChatRoom() throws Exception {
      // given
      setUpSecurityContext();
      Long nonExistentChatRoomId = 999L;

      doThrow(new RuntimeException("채팅방을 찾을 수 없습니다."))
          .when(tripUseCase)
          .startTripPlanGeneration(TEST_EMAIL, nonExistentChatRoomId);

      // when & then
      mockMvc
          .perform(post("/api/v1/trips/{chatRoomId}", nonExistentChatRoomId))
          .andDo(print())
          .andExpect(status().is5xxServerError());

      verify(tripUseCase).startTripPlanGeneration(TEST_EMAIL, nonExistentChatRoomId);
    }

    @Test
    @DisplayName("given 다양한 채팅방 ID when 여행 계획 생성 시작 then 각각의 ID로 서비스가 호출된다")
    void startTripPlanGenerationWithDifferentChatRoomIds() throws Exception {
      // given
      setUpSecurityContext();
      Long[] chatRoomIds = {1L, 100L, 9999L};

      for (Long chatRoomId : chatRoomIds) {
        doNothing().when(tripUseCase).startTripPlanGeneration(TEST_EMAIL, chatRoomId);

        // when & then
        mockMvc
            .perform(post("/api/v1/trips/{chatRoomId}", chatRoomId))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.message").value("여행 일정 생성을 시작합니다."));

        verify(tripUseCase).startTripPlanGeneration(TEST_EMAIL, chatRoomId);
      }
    }
  }
}
