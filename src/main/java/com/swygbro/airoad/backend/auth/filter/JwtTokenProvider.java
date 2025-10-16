package com.swygbro.airoad.backend.auth.filter;

import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.auth.application.TokenService;
import com.swygbro.airoad.backend.auth.domain.dto.TokenResponse;
import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.auth.domain.principal.UserDetailsServiceImpl;
import com.swygbro.airoad.backend.auth.domain.principal.UserPrincipal;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  @Value("${jwt.access-token-secret}")
  private String accessTokenSecret;

  @Value("${jwt.refresh-token-secret}")
  private String refreshTokenSecret;

  @Value("${jwt.access-token-expiration}")
  private long accessTokenExpiration;

  @Value("${jwt.refresh-token-expiration}")
  private long refreshTokenExpiration;

  private final UserDetailsServiceImpl userDetailsService;

  private final TokenService tokenService;

  public TokenResponse generateToken(Authentication authentication) {
    String accessToken = createAccessToken(authentication);
    String refreshToken = createRefreshToken(authentication);
    Date accessTokenExpiryDate = createExpiryDate(accessTokenExpiration);
    return TokenResponse.from(
        accessToken, refreshToken, accessTokenExpiryDate.getTime(), refreshTokenExpiration);
  }

  public String createAccessToken(Authentication authentication) {
    return createToken(authentication, accessTokenExpiration, accessTokenSecret);
  }

  public String createRefreshToken(Authentication authentication) {
    String token = createToken(authentication, refreshTokenExpiration, refreshTokenSecret);
    Date expiryDate = createExpiryDate(refreshTokenExpiration);
    tokenService.createRefreshToken(token, expiryDate, authentication);
    return token;
  }

  private String createToken(Authentication authentication, long expirationTime, String secretKey) {
    String authorities =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

    return Jwts.builder()
        .setSubject(authentication.getName()) // email
        .claim("auth", authorities)
        .setIssuedAt(new Date())
        .setExpiration(createExpiryDate(expirationTime))
        .signWith(createKey(secretKey))
        .compact();
  }

  public String getUsernameFromToken(String token, TokenType tokenType) {
    String secretKey =
        (tokenType == TokenType.ACCESS_TOKEN) ? accessTokenSecret : refreshTokenSecret;

    return Jwts.parserBuilder()
        .setSigningKey(createKey(secretKey))
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  public Authentication getAuthentication(String token, TokenType tokenType) {
    String username = getUsernameFromToken(token, tokenType);
    UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(username);

    return new UsernamePasswordAuthenticationToken(
        userPrincipal, token, userPrincipal.getAuthorities());
  }

  public boolean validateAccessToken(String token) {
    return validateToken(token, accessTokenSecret);
  }

  public boolean validateRefreshToken(String token) {
    return validateToken(token, refreshTokenSecret);
  }

  private boolean validateToken(String token, String secretKey) {
    try {
      Jwts.parserBuilder().setSigningKey(createKey(secretKey)).build().parseClaimsJws(token);
      return true;
    } catch (ExpiredJwtException e) {
      log.error("만료된 JWT 토큰입니다.");
      throw e;
    } catch (SecurityException | MalformedJwtException e) {
      log.error("잘못된 JWT 서명입니다.");
    } catch (UnsupportedJwtException e) {
      log.error("지원되지 않는 JWT 토큰입니다.");
    } catch (IllegalArgumentException e) {
      log.error("JWT 토큰이 잘못되었습니다.");
    }
    return false;
  }

  private Date createExpiryDate(long expirationTime) {
    return new Date(System.currentTimeMillis() + expirationTime);
  }

  private SecretKey createKey(String secret) {
    byte[] keyBytes = Decoders.BASE64URL.decode(secret);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
