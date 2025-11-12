package com.swygbro.airoad.backend.ai.agent.trip.converter;

import org.springframework.ai.converter.BeanOutputConverter;

public class NdJsonBeanOutPutConverter<T> extends BeanOutputConverter<T> {

  public NdJsonBeanOutPutConverter(Class<T> clazz) {
    super(clazz);
  }

  @Override
  public String getFormat() {
    String template =
        """
        ## 출력 형식: NDJSON (Newline Delimited JSON)
        **중요**: 반드시 NDJSON 형식으로만 응답해야 합니다.

        - 출력은 **오직 NDJSON 데이터**로만 구성되어야 하며, 추가 텍스트, 공백, 주석, 설명을 절대로 포함하지 마세요.
        - 지정된 형식에 맞는 **RFC8259 규격**을 준수하는 JSON 응답만 제공하세요.
        - 응답에 **마크다운 코드 블록(예: ```json)** 을 포함하지 마세요.
        - 출력에서 **모든 마크다운 문법(예: ```json, ```, #, **)** 을 제거하세요.
        - 각 줄(line)에는 **정확히 하나의 완전한 JSON 객체**만 포함해야 합니다.
        - 각 JSON 객체는 줄바꿈 문자(`\\n`)로만 구분되어야 합니다.
        - 각 JSON 객체는 독립적으로 파싱 가능해야 합니다.
        - 절대로 배열(`[]`)이나 부모 객체로 감싸지 마세요.
        - JSON Schema를 출력하지 말고, 스키마 형식에 맞는 실제 데이터를 생성하세요.

        아래는 당신의 출력이 반드시 따라야 하는 JSON 스키마입니다:
        ```json
        %s
        ```
        """;

    return String.format(template, this.getJsonSchema());
  }
}
