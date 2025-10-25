package com.swygbro.airoad.backend.chat.fixture;

import java.time.LocalDate;

import org.springframework.test.util.ReflectionTestUtils;

import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;
import com.swygbro.airoad.backend.member.domain.entity.ProviderType;
import com.swygbro.airoad.backend.trip.domain.entity.Transportation;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

/**
 * AiConversation 엔티티 테스트 픽스처
 *
 * <p>테스트에서 AiConversation 객체를 쉽게 생성하기 위한 유틸리티 클래스입니다.
 */
public class AiConversationFixture {

  /**
   * 기본 AiConversation 객체 생성
   *
   * @param id 대화 세션 ID
   * @return AiConversation 객체
   */
  public static AiConversation createConversation(Long id) {
    AiConversation conversation = AiConversation.builder().member(null).tripPlan(null).build();
    ReflectionTestUtils.setField(conversation, "id", id);
    return conversation;
  }

  /**
   * ID와 사용자 이메일을 가진 AiConversation 객체 생성
   *
   * @param id 대화 세션 ID
   * @param userEmail 사용자 이메일
   * @return AiConversation 객체
   */
  public static AiConversation createConversation(Long id, String userEmail) {
    Member member =
        Member.builder()
            .email(userEmail)
            .name("테스트유저")
            .imageUrl("https://example.com/image.png")
            .provider(ProviderType.GOOGLE)
            .role(MemberRole.MEMBER)
            .build();

    TripPlan tripPlan =
        TripPlan.builder()
            .member(member)
            .title("테스트 여행")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(3))
            .isCompleted(false)
            .region("서울")
            .transportation(Transportation.PUBLIC_TRANSIT)
            .peopleCount(2)
            .build();
    ReflectionTestUtils.setField(tripPlan, "id", 1L);

    AiConversation conversation =
        AiConversation.builder().member(member).tripPlan(tripPlan).build();
    ReflectionTestUtils.setField(conversation, "id", id);
    return conversation;
  }

  /**
   * 기본 ID(1L)를 가진 AiConversation 객체 생성
   *
   * @return AiConversation 객체
   */
  public static AiConversation createConversation() {
    return AiConversation.builder().build();
  }
}
