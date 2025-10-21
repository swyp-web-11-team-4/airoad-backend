package com.swygbro.airoad.backend.common.domain.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 커서 기반 페이징 응답 DTO
 *
 * <p>무한 스크롤이나 채팅 메시지 이력 조회 등에 사용되는 커서 기반 페이징을 위한 공통 응답 클래스입니다.
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "커서 기반 페이징 응답")
public class CursorPageResponse<T> {

  @Schema(description = "조회된 데이터 목록")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<T> content;

  @Schema(description = "다음 페이지 조회를 위한 커서 (다음 페이지가 없으면 null)", example = "123")
  private Long nextCursor;

  @Schema(description = "다음 페이지 존재 여부", example = "true")
  private boolean hasNext;

  @Schema(description = "현재 페이지 데이터 개수", example = "20")
  private int size;

  /**
   * CursorPageResponse 생성 팩토리 메서드
   *
   * @param content 조회된 데이터 목록
   * @param nextCursor 다음 페이지 조회를 위한 커서 (없으면 null)
   * @param hasNext 다음 페이지 존재 여부
   * @param <T> 응답 데이터 타입
   * @return CursorPageResponse 인스턴스
   */
  public static <T> CursorPageResponse<T> of(List<T> content, Long nextCursor, boolean hasNext) {
    return new CursorPageResponse<>(content, nextCursor, hasNext, content.size());
  }

  /**
   * 다음 페이지가 없는 마지막 페이지 생성 팩토리 메서드
   *
   * @param content 조회된 데이터 목록
   * @param <T> 응답 데이터 타입
   * @return CursorPageResponse 인스턴스
   */
  public static <T> CursorPageResponse<T> last(List<T> content) {
    return new CursorPageResponse<>(content, null, false, content.size());
  }
}
