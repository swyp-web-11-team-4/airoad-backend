package com.swygbro.airoad.backend.trip.domain.dto.request;

import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.swygbro.airoad.backend.trip.domain.entity.TripPlan;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TripPlanSortField {
  CREATED_AT("createdAt", TripPlan::getCreatedAt),
  START_DATE("startDate", TripPlan::getStartDate);

  private final String fieldName;
  private final Function<TripPlan, Comparable<?>> cursorValueExtractor;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Specification<TripPlan> getCursorSpecification(TripPlan cursor, Sort.Direction direction) {
    return (root, query, cb) -> {
      Comparable cursorValue = cursorValueExtractor.apply(cursor);
      if (direction == Sort.Direction.DESC) {
        return cb.or(
            cb.lessThan(root.get(fieldName), cursorValue),
            cb.and(
                cb.equal(root.get(fieldName), cursorValue),
                cb.lessThan(root.get("id"), cursor.getId())));
      } else {
        return cb.or(
            cb.greaterThan(root.get(fieldName), cursorValue),
            cb.and(
                cb.equal(root.get(fieldName), cursorValue),
                cb.greaterThan(root.get("id"), cursor.getId())));
      }
    };
  }

  public static TripPlanSortField from(String fieldName) {
    return Stream.of(values())
        .filter(field -> field.getFieldName().equals(fieldName))
        .findFirst()
        .orElse(CREATED_AT);
  }
}
