package com.swygbro.airoad.backend.chat.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swygbro.airoad.backend.chat.domain.entity.AiConversation;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

  @Query(
      "SELECT ac.tripPlan.id AS tripPlanId, ac.id AS conversationId "
          + "FROM AiConversation ac WHERE ac.tripPlan.id IN :tripPlanIds")
  List<ConversationIdProjection> findConversationIdsByTripPlanIds(
      @Param("tripPlanIds") List<Long> tripPlanIds);

  @Query(
      "SELECT ac FROM AiConversation ac join fetch ac.tripPlan tp join fetch tp.tripThemes WHERE ac.tripPlan.id = :tripPlanId")
  Optional<AiConversation> findByTripPlanId(@Param("tripPlanId") Long tripPlanId);

  @Modifying
  @Query("DELETE FROM AiConversation ac WHERE ac.tripPlan.id = :tripPlanId")
  void deleteByTripPlanId(Long tripPlanId);
}
