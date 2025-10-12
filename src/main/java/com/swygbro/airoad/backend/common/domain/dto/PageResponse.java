package com.swygbro.airoad.backend.common.domain.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PageResponse", description = "페이징 응답")
public record PageResponse<T>(
    @Schema(description = "데이터 목록", requiredMode = Schema.RequiredMode.REQUIRED) List<T> content,
    @Schema(
            description = "현재 페이지 번호 (0부터 시작)",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int page,
    @Schema(
            description = "페이지당 요청된 데이터 개수",
            example = "10",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int size,
    @Schema(description = "전체 데이터 개수", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        long totalElements,
    @Schema(description = "전체 페이지 수", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        int totalPages,
    @Schema(
            description = "현재 페이지의 실제 데이터 개수",
            example = "10",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int numberOfElements,
    @Schema(
            description = "첫 번째 페이지 여부",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean first,
    @Schema(
            description = "마지막 페이지 여부",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean last,
    @Schema(
            description = "데이터가 비어있는지 여부",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean empty,
    @Schema(
            description = "다음 페이지 존재 여부",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasNext,
    @Schema(
            description = "이전 페이지 존재 여부",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasPrevious) {

  /**
   * Spring Data Page 객체를 PageResponse로 변환합니다.
   *
   * @param page Spring Data Page 객체
   * @param <T> 데이터 타입
   * @return PageResponse 객체
   */
  public static <T> PageResponse<T> of(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.getNumberOfElements(),
        page.isFirst(),
        page.isLast(),
        page.isEmpty(),
        page.hasNext(),
        page.hasPrevious());
  }
}
