package com.swygbro.airoad.backend.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.swygbro.airoad.backend.auth.application.CustomOAuth2UserService;
import com.swygbro.airoad.backend.auth.filter.JwtAuthenticationFilter;
import com.swygbro.airoad.backend.auth.infrastructure.CustomOAuth2AuthorizationRequestRepository;
import com.swygbro.airoad.backend.auth.presentation.web.JwtAuthenticationEntryPoint;
import com.swygbro.airoad.backend.auth.presentation.web.OAuth2AuthenticationFailureHandler;
import com.swygbro.airoad.backend.auth.presentation.web.OAuth2AuthenticationSuccessHandler;
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;

import lombok.RequiredArgsConstructor;

import static java.util.Arrays.asList;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

  @Value("${cors.allowed-origins}")
  private String allowedOrigins;

  private final CustomOAuth2UserService customOAuth2UserService;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
  private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
  private final CustomOAuth2AuthorizationRequestRepository
      customOAuth2AuthorizationRequestRepository;

  @Bean
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
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers("/oauth2/**", "/login/oauth2/**")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    .requestMatchers("/api/admin/tourdata/**")
                    .permitAll()
                    .requestMatchers("/api/v1/**")
                    .hasRole(MemberRole.MEMBER.getRole())
                    .anyRequest()
                    .authenticated())
        .oauth2Login(
            oauth2 ->
                oauth2
                    .authorizationEndpoint(
                        authorization ->
                            authorization.authorizationRequestRepository(
                                customOAuth2AuthorizationRequestRepository))
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler(oAuth2AuthenticationFailureHandler)
                    .userInfoEndpoint(config -> config.userService(customOAuth2UserService)))
        .exceptionHandling(
            exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(asList(allowedOrigins.split(",")));
    config.setAllowedMethods(asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(
        asList("Authorization", "Content-Type", "Accept", "Cookie", "Origin", "Referer"));
    config.setExposedHeaders(asList("Set-Cookie", "Authorization"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
