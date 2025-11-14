package com.swygbro.airoad.backend.ai.domain.dto.context;

import lombok.Builder;

/**
 * 스트림 기반 출력 처리를 위한 컨텍스트
 *
 * <p>이 클래스는 스트림 데이터를 출력하는 데 필요한 컨텍스트 정보를 제공하는 역할을 합니다. 특정 출력 프로세스 또는 스트림 기반 동작에서 동작 정보를 캡슐화합니다.
 */
@Builder
public record StreamOutputContext(String jsonSchema) {}
