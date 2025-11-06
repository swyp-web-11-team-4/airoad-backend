package com.swygbro.airoad.backend.chat.infrastructure;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.chat.infrastructure.repository.AiConversationRepository;
import com.swygbro.airoad.backend.fixture.chat.AiConversationFixture;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.member.infrastructure.MemberRepository;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;
import com.swygbro.airoad.backend.trip.infrastructure.TripPlanRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiConversationRepository 테스트
 *
 * <p>Repository 계층 테스트로 실제 데이터베이스와의 상호작용을 검증합니다.
 */
@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class AiConversationRepositoryTest {

  @Autowired private AiConversationRepository aiConversationRepository;

  @Autowired private MemberRepository memberRepository;

  @Autowired private TripPlanRepository tripPlanRepository;

  private Member testMember;
  private TripPlan testTripPlan;
  private AiConversation testConversation;

  @BeforeEach
  void setUp() {
    // given: Fixture를 사용하여 테스트 데이터 생성 및 저장
    testMember = memberRepository.save(MemberFixture.create());
    testTripPlan = tripPlanRepository.save(TripPlanFixture.createWithMember(testMember));
    testConversation =
        aiConversationRepository.save(
            AiConversationFixture.createWithMemberAndTripPlan(testMember, testTripPlan));
  }

  @AfterEach
  void tearDown() {
    aiConversationRepository.deleteAllInBatch();
    tripPlanRepository.deleteAllInBatch();
    memberRepository.deleteAllInBatch();
  }

  @Nested
  @DisplayName("findAiConversationByTripPlanId 메서드는")
  class FindAiConversationByTripPlanId {

    @Test
    @DisplayName("tripPlanId로 AI 대화를 정상적으로 조회한다")
    void shouldFindAiConversationByTripPlanId() {
      // given: 이미 setUp에서 testTripPlan과 testConversation이 생성됨
      Long tripPlanId = testTripPlan.getId();

      // when: tripPlanId로 AiConversation 조회
      Optional<AiConversation> result = aiConversationRepository.findByTripPlanId(tripPlanId);

      // then: AiConversation이 조회됨
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(testConversation.getId());
      assertThat(result.get().getTripPlanId()).isEqualTo(tripPlanId);
      assertThat(result.get().getMember().getId()).isEqualTo(testMember.getId());
    }

    @Test
    @DisplayName("존재하지 않는 tripPlanId로 조회 시 빈 Optional을 반환한다")
    void shouldReturnEmptyOptionalWhenTripPlanIdNotExists() {
      // given: 존재하지 않는 tripPlanId
      Long nonExistentTripPlanId = 99999L;

      // when: 존재하지 않는 tripPlanId로 조회
      Optional<AiConversation> result =
          aiConversationRepository.findByTripPlanId(nonExistentTripPlanId);

      // then: 빈 Optional 반환
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("여러 대화가 존재할 때 특정 tripPlanId의 대화만 조회한다")
    void shouldFindOnlyConversationForSpecificTripPlan() {
      // given: 다른 회원과 여행 계획으로 추가 대화 생성
      Member anotherMember =
          memberRepository.save(MemberFixture.createWithEmail("another@example.com"));
      TripPlan anotherTripPlan =
          tripPlanRepository.save(TripPlanFixture.createWithMember(anotherMember));
      AiConversation anotherConversation =
          aiConversationRepository.save(
              AiConversationFixture.createWithMemberAndTripPlan(anotherMember, anotherTripPlan));

      // when: 첫 번째 tripPlanId로 조회
      Optional<AiConversation> result =
          aiConversationRepository.findByTripPlanId(testTripPlan.getId());

      // then: 첫 번째 대화만 조회됨
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(testConversation.getId());
      assertThat(result.get().getId()).isNotEqualTo(anotherConversation.getId());
    }

    @Test
    @DisplayName("대화가 삭제된 경우 빈 Optional을 반환한다")
    void shouldReturnEmptyOptionalWhenConversationIsDeleted() {
      // given: 대화 삭제
      Long tripPlanId = testTripPlan.getId();
      aiConversationRepository.delete(testConversation);
      aiConversationRepository.flush();

      // when: 삭제된 대화의 tripPlanId로 조회
      Optional<AiConversation> result = aiConversationRepository.findByTripPlanId(tripPlanId);

      // then: 빈 Optional 반환
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("지연 로딩된 연관 엔티티(member, tripPlan)에 접근할 수 있다")
    void shouldAccessLazyLoadedAssociations() {
      // given: tripPlanId로 대화 조회
      Long tripPlanId = testTripPlan.getId();

      // when: AiConversation 조회 후 연관 엔티티 접근
      Optional<AiConversation> result = aiConversationRepository.findByTripPlanId(tripPlanId);

      // then: 지연 로딩된 연관 엔티티에 접근 가능
      assertThat(result).isPresent();
      AiConversation conversation = result.get();

      // member 엔티티 접근
      assertThat(conversation.getMember()).isNotNull();
      assertThat(conversation.getMember().getId()).isEqualTo(testMember.getId());

      // tripPlan 엔티티 접근
      assertThat(conversation.getTripPlan()).isNotNull();
      assertThat(conversation.getTripPlan().getId()).isEqualTo(testTripPlan.getId());
    }
  }
}
