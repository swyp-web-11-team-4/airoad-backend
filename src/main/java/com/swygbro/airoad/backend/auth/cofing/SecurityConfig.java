package com.swygbro.airoad.backend.auth.cofing;

import java.util.Arrays;
import java.util.List;

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

import lombok.RequiredArgsConstructor;

import static java.util.Arrays.*;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;

  private static final String[] SWAGGER_URLS = {
    "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**"
  };

  private static final String[] PUBLIC_URLS = {
    "/api/v1/auth/login", "/api/v1/auth/reissue", "/api/v1/auth/logout"
  };

  private static final String[] MEMBER_URLS = {"/api/v1/members/**"};

  private static final String[] CHATS_URLS = {"/api/v1/chats/**"};

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(SWAGGER_URLS).permitAll()
            .requestMatchers(PUBLIC_URLS).permitAll()
            .requestMatchers(MEMBER_URLS).hasRole("MEMBER")
            .requestMatchers(CHATS_URLS).hasRole("MEMBER")
            .anyRequest().authenticated())
        .oauth2Login(oauth2 -> oauth2.successHandler(oAuthLoginSuccessHandler))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(asList("http://localhost:3000", "http://127.0.0.1:3000"));
    configuration.setAllowedMethods(asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowedHeaders(asList("Authorization", "Content-Type", "Accept", "Set-Cookie"));
    configuration.setExposedHeaders(asList("Set-Cookie", "Authorization"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
