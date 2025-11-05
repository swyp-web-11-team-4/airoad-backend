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
import com.swygbro.airoad.backend.common.presentation.GlobalExceptionHandler;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.trip.application.TripPlanUseCase;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanCreateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.request.TripPlanUpdateRequest;
import com.swygbro.airoad.backend.trip.domain.dto.response.TripPlanResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@DisplayName("TripPlanController 테스트")
class TripPlanControllerTest {

  @Mock private TripPlanUseCase tripPlanUseCase;

  @InjectMocks private TripPlanController tripPlanController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(tripPlanController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
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
  @DisplayName("여행 일정 생성 요청 API 호출 시")
  class GenerateTripPlan {

    @Test
    @DisplayName("인증된 사용자가 여행 일정 생성을 성공적으로 요청한다")
    void requestsTripPlanGenerationSuccessfully() throws Exception {
      // given
      Long chatRoomId = 1L;
      TripPlanCreateRequest request =
          new TripPlanCreateRequest(List.of("맛집"), LocalDate.now(), 3, "서울", 2);
      willDoNothing()
          .given(tripPlanUseCase)
          .requestTripPlanGeneration(anyString(), any(TripPlanCreateRequest.class), anyLong());

      // when & then
      mockMvc
          .perform(
              post("/api/v1/trips")
                  .param("chatRoomId", String.valueOf(chatRoomId))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isAccepted())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.message").value("여행 일정 생성이 시작되었습니다."));
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
}
