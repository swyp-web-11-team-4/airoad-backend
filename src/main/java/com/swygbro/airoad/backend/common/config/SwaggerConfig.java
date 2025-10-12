package com.swygbro.airoad.backend.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    paramName = "Authorization")
public class SwaggerConfig {

  @Value("${swagger.url:http://localhost:8080}")
  private String url;

  @Bean
  public OpenAPI openAPI() {
    Contact contact =
        new Contact().name("SWYP Web 11기 Team 4").url("https://github.com/swyp-web-11-team-4");

    Info info =
        new Info()
            .title("API Documentation")
            .description(
                """
                ## SWYP Web 11기 Team 4 Backend API

                이 문서는 백엔드 REST API의 전체 엔드포인트를 설명합니다.

                ### 인증 방식
                - JWT Bearer Token 인증을 사용합니다.
                - 대부분의 API는 인증이 필요하며, 우측 상단의 'Authorize' 버튼을 통해 토큰을 설정할 수 있습니다.

                ### 공통 응답 형식
                - 모든 응답: `CommonResponse<T>` 래퍼 사용
                  ```json
                  {
                    "success": true,
                    "status": 200,
                    "data": {
                      ...
                    }
                  }
                  ```

                ### 응답 상태 코드
                - `200 OK`: 요청 성공
                - `201 Created`: 리소스 생성 성공
                - `400 Bad Request`: 잘못된 요청
                - `401 Unauthorized`: 인증 실패
                - `404 Not Found`: 리소스를 찾을 수 없음
                - `500 Internal Server Error`: 서버 내부 오류
                """)
            .contact(contact);

    Server server = new Server().url(url);

    SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

    return new OpenAPI().info(info).servers(List.of(server)).addSecurityItem(securityRequirement);
  }
}
