package com.swygbro.airoad.backend.auth.cofing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
import com.swygbro.airoad.backend.member.domain.entity.MemberRole;

import lombok.RequiredArgsConstructor;

import static java.util.Arrays.*;

@Profile("!test")
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;

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
        .oauth2Login(oauth2 -> oauth2.successHandler(oAuthLoginSuccessHandler))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // My Local Server Test URL
    configuration.setAllowedOrigins(asList("http://localhost:3000", "http://127.0.0.1:3000"));
    configuration.setAllowedOrigins(asList("http://localhost:5173", "http://127.0.0.1:5173" , "https://airoad.linguaflow.store"));
    configuration.setAllowedMethods(asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(asList("Authorization", "Content-Type", "Accept", "Cookie"));
    configuration.setExposedHeaders(asList("Set-Cookie", "Authorization"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
