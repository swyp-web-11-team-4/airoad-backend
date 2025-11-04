package com.swygbro.airoad.backend.trip.presentation.web;

import java.time.LocalDate;
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
import com.swygbro.airoad.backend.common.presentation.GlobalExceptionHandler;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.trip.application.TripUseCase;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.doNothing;
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

  @Nested
  @DisplayName("generateTripPlan 메서드는")
  class GenerateTripPlan {

    @Test
    @DisplayName("유효한 요청으로 여행 일정 생성을 요청하면 202 Accepted를 반환한다")
    void shouldReturn202WhenValidRequest() throws Exception {
      // given
      setupAuthentication();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("제주")
              .startDate(LocalDate.of(2025, 3, 1))
              .duration(3)
              .themes(List.of("힐링", "맛집"))
              .peopleCount(2)
              .build();

      doNothing()
          .when(tripUseCase)
          .requestTripPlanGeneration(anyString(), any(TripPlanCreateRequest.class), anyLong());

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .param("chatRoomId", String.valueOf(TEST_CHAT_ROOM_ID))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isAccepted())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(202))
          .andExpect(jsonPath("$.data.message").value("여행 일정 생성이 시작되었습니다."));

      verify(tripUseCase, times(1))
          .requestTripPlanGeneration(TEST_EMAIL, request, TEST_CHAT_ROOM_ID);
    }

    @Test
    @DisplayName("region이 누락되면 400 Bad Request를 반환한다")
    void shouldReturn400WhenRegionIsMissing() throws Exception {
      // given
      setupAuthentication();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region(null) // 누락
              .startDate(LocalDate.of(2025, 3, 1))
              .duration(3)
              .themes(List.of("힐링", "맛집"))
              .peopleCount(2)
              .build();

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .param("chatRoomId", String.valueOf(TEST_CHAT_ROOM_ID))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(400));

      verify(tripUseCase, times(0))
          .requestTripPlanGeneration(anyString(), any(TripPlanCreateRequest.class), anyLong());
    }

    @Test
    @DisplayName("startDate가 누락되면 400 Bad Request를 반환한다")
    void shouldReturn400WhenStartDateIsMissing() throws Exception {
      // given
      setupAuthentication();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("제주")
              .startDate(null) // 누락
              .duration(3)
              .themes(List.of("힐링", "맛집"))
              .peopleCount(2)
              .build();

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .param("chatRoomId", String.valueOf(TEST_CHAT_ROOM_ID))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(400));

      verify(tripUseCase, times(0))
          .requestTripPlanGeneration(anyString(), any(TripPlanCreateRequest.class), anyLong());
    }

    @Test
    @DisplayName("duration이 0 이하면 400 Bad Request를 반환한다")
    void shouldReturn400WhenDurationIsZeroOrNegative() throws Exception {
      // given
      setupAuthentication();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("제주")
              .startDate(LocalDate.of(2025, 3, 1))
              .duration(0) // 유효하지 않은 값
              .themes(List.of("힐링", "맛집"))
              .peopleCount(2)
              .build();

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .param("chatRoomId", String.valueOf(TEST_CHAT_ROOM_ID))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(400));

      verify(tripUseCase, times(0))
          .requestTripPlanGeneration(anyString(), any(TripPlanCreateRequest.class), anyLong());
    }

    @Test
    @DisplayName("themes가 비어있으면 400 Bad Request를 반환한다")
    void shouldReturn400WhenThemesIsEmpty() throws Exception {
      // given
      setupAuthentication();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("제주")
              .startDate(LocalDate.of(2025, 3, 1))
              .duration(3)
              .themes(List.of()) // 비어있음
              .peopleCount(2)
              .build();

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .param("chatRoomId", String.valueOf(TEST_CHAT_ROOM_ID))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(400));

      verify(tripUseCase, times(0))
          .requestTripPlanGeneration(anyString(), any(TripPlanCreateRequest.class), anyLong());
    }

    @Test
    @DisplayName("peopleCount가 0 이하면 400 Bad Request를 반환한다")
    void shouldReturn400WhenPeopleCountIsZeroOrNegative() throws Exception {
      // given
      setupAuthentication();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("제주")
              .startDate(LocalDate.of(2025, 3, 1))
              .duration(3)
              .themes(List.of("힐링", "맛집"))
              .peopleCount(0) // 유효하지 않은 값
              .build();

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .param("chatRoomId", String.valueOf(TEST_CHAT_ROOM_ID))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.status").value(400));

      verify(tripUseCase, times(0))
          .requestTripPlanGeneration(anyString(), any(TripPlanCreateRequest.class), anyLong());
    }

    @Test
    @DisplayName("chatRoomId가 누락되면 400 Bad Request를 반환한다")
    void shouldReturn400WhenChatRoomIdIsMissing() throws Exception {
      // given
      setupAuthentication();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("제주")
              .startDate(LocalDate.of(2025, 3, 1))
              .duration(3)
              .themes(List.of("힐링", "맛집"))
              .peopleCount(2)
              .build();

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  // chatRoomId 파라미터 누락
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isBadRequest());

      verify(tripUseCase, times(0))
          .requestTripPlanGeneration(anyString(), any(TripPlanCreateRequest.class), anyLong());
    }

    @Test
    @DisplayName("여러 테마로 요청하면 성공적으로 처리한다")
    void shouldHandleMultipleThemes() throws Exception {
      // given
      setupAuthentication();

      TripPlanCreateRequest request =
          TripPlanCreateRequest.builder()
              .region("부산")
              .startDate(LocalDate.of(2025, 4, 15))
              .duration(5)
              .themes(List.of("힐링", "맛집", "액티비티", "문화"))
              .peopleCount(4)
              .build();

      doNothing()
          .when(tripUseCase)
          .requestTripPlanGeneration(anyString(), any(TripPlanCreateRequest.class), anyLong());

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .param("chatRoomId", String.valueOf(TEST_CHAT_ROOM_ID))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isAccepted())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.status").value(202));

      verify(tripUseCase, times(1))
          .requestTripPlanGeneration(TEST_EMAIL, request, TEST_CHAT_ROOM_ID);
    }
  }

  private void setupAuthentication() {
    Member member =
        Member.builder()
            .email(TEST_EMAIL)
            .name("테스트")
            .imageUrl("https://example.com/image.jpg")
            .provider(ProviderType.GOOGLE)
            .role(MemberRole.MEMBER)
            .build();

    UserPrincipal userPrincipal = new UserPrincipal(member);

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            userPrincipal, null, userPrincipal.getAuthorities());

    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    SecurityContextHolder.setContext(securityContext);
  }
}
