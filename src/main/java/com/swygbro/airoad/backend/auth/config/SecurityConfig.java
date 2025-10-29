package com.swygbro.airoad.backend.auth.config;

import java.util.Collections;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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

import static java.util.Arrays.asList;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  @Value("${cors.allowed-origins}")
  private String allowedOrigins;

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;
  private final HttpCookieOAuth2AuthorizationRequestRepository
      httpCookieOAuth2AuthorizationRequestRepository;

  @Bean
  @Profile({"local", "dev"})
  public SecurityFilterChain localDevFilterChain(HttpSecurity http) throws Exception {
    http.cors(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/ws-stomp/**")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    .requestMatchers("/api/v1/**")
                    .hasRole(MemberRole.MEMBER.getRole())
                    .anyRequest()
                    .permitAll())
        .oauth2Login(oauth2 -> oauth2.successHandler(oAuthLoginSuccessHandler))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  @Profile("prod")
  public SecurityFilterChain prodFilterChain(HttpSecurity http) throws Exception {
    http.cors(c -> c.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/ws-stomp/**")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    .requestMatchers("/api/v1/**")
                    .hasRole(MemberRole.MEMBER.getRole())
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
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Collections.singletonList(allowedOrigins));
    config.setAllowedMethods(asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(asList("Authorization", "Content-Type", "Accept", "Cookie"));
    config.setExposedHeaders(asList("Set-Cookie", "Authorization"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
