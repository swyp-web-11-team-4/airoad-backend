package com.swygbro.airoad.backend.realtime;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일정 구독 테스트용 컨트롤러
 *
 * <p>이 컨트롤러는 {@code /user/sub/schedule} 구독이 정상 작동하는지 테스트하기 위한 임시 컨트롤러입니다.
 *
 * <h3>테스트 방법</h3>
 *
 * <ol>
 *   <li>클라이언트가 {@code /user/sub/schedule}를 구독
 *   <li>클라이언트가 {@code /pub/test/schedule}로 메시지 전송
 *   <li>서버가 구독한 사용자에게 더미 일정 데이터 전송
 * </ol>
 *
 * <p><strong>주의</strong>: 실제 프로덕션에서는 삭제되어야 할 테스트 코드입니다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ScheduleTestController {

  private final SimpMessagingTemplate messagingTemplate;

  /**
   * 일정 구독 테스트용 엔드포인트
   *
   * <p>클라이언트가 {@code /pub/test/schedule}로 메시지를 보내면 {@code /user/sub/schedule}로 더미 일정 데이터를 전송합니다.
   *
   * @param principal 인증된 사용자 정보
   */
  @MessageMapping("/test/schedule")
  public void testScheduleSubscription(Principal principal) {
    String userId = principal != null ? principal.getName() : "anonymous";

    log.info("[Test] 일정 구독 테스트 요청 - userId: {}", userId);

    // 더미 일정 데이터 생성
    ScheduleResponse scheduleResponse =
        new ScheduleResponse(
            1L,
            "서울 3일 여행",
            LocalDate.now().plusDays(1),
            List.of(
                "09:00 경복궁 방문",
                "12:00 북촌 한옥마을 점심",
                "15:00 인사동 카페 투어",
                "18:00 명동 저녁 식사",
                "20:00 남산타워 야경 감상"),
            LocalDateTime.now());

    // /user/{userId}/sub/schedule로 전송
    String destination = "/sub/schedule";
    messagingTemplate.convertAndSendToUser(userId, destination, scheduleResponse);

    log.info("[Test] 일정 데이터 전송 완료 - userId: {}, destination: {}", userId, destination);
  }
}
