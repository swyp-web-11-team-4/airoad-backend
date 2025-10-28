package com.swygbro.airoad.backend.auth.config;

import java.util.Collections;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.swygbro.airoad.backend.auth.application.OAuthLoginSuccessHandler;
import com.swygbro.airoad.backend.auth.filter.JwtAuthenticationFilter;
import com.swygbro.airoad.backend.auth.infrastructure.HttpCookieOAuth2AuthorizationRequestRepository;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;

import lombok.RequiredArgsConstructor;

import static java.util.Arrays.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  @Value("${cors.allowed-origins}")
  private String allowedOrigins;

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;
  private final HttpCookieOAuth2AuthorizationRequestRepository
      httpCookieOAuth2AuthorizationRequestRepository;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/api/v1/members/**")
                    .hasRole(MemberRole.MEMBER.getRole())
                    .requestMatchers("/api/v1/chats/**")
                    .hasRole(MemberRole.MEMBER.getRole())
                    .requestMatchers("/ws-stomp/**")
                    .permitAll() // WebSocket Handshake는 인증 없이 허용, STOMP CONNECT에서 인증
                    .requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/swagger-resources/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(
            oauth2 ->
                oauth2
                    // 쿠키 기반 Authorization Request 저장소 사용
                    .authorizationEndpoint(
                        authorization ->
                            authorization.authorizationRequestRepository(
                                httpCookieOAuth2AuthorizationRequestRepository))
                    // OAuth2 로그인 성공 시 JWT 발급
                    .successHandler(oAuthLoginSuccessHandler))
        // JWT 인증 필터 추가
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Collections.singletonList(allowedOrigins));
    configuration.setAllowedMethods(asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(asList("Authorization", "Content-Type", "Accept", "Cookie"));
    configuration.setExposedHeaders(asList("Set-Cookie", "Authorization"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
