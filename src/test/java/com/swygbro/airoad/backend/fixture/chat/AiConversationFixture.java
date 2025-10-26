package com.swygbro.airoad.backend.fixture.chat;

import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;
import com.swygbro.airoad.backend.fixture.member.MemberFixture;
import com.swygbro.airoad.backend.fixture.trip.TripPlanFixture;
import com.swygbro.airoad.backend.member.domain.entity.Member;
import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

public class AiConversationFixture {

  public static AiConversation create() {
    return AiConversation.builder()
        .member(MemberFixture.create())
        .tripPlan(TripPlanFixture.create())
        .build();
  }

  public static AiConversation createWithMember(Member member) {
    return AiConversation.builder().member(member).tripPlan(TripPlanFixture.create()).build();
  }

  public static AiConversation createWithTripPlan(TripPlan tripPlan) {
    return AiConversation.builder().member(MemberFixture.create()).tripPlan(tripPlan).build();
  }

  public static AiConversation createWithMemberAndTripPlan(Member member, TripPlan tripPlan) {
    return AiConversation.builder().member(member).tripPlan(tripPlan).build();
  }

  public static AiConversation.AiConversationBuilder builder() {
    return AiConversation.builder()
        .member(MemberFixture.create())
        .tripPlan(TripPlanFixture.create());
  }
}
