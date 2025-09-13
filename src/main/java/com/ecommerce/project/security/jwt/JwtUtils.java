package com.ecommerce.project.security.jwt;

import com.ecommerce.project.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    @Value("${spring.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${spring.ecom.app.jwtCookieName}")
    private String jwtCookieName;

    private ResponseCookie deleteCookieAt(String name, String path, boolean httpOnly, boolean secure, String sameSite) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, "");
        b.path(path).maxAge(0).httpOnly(httpOnly).secure(secure);
        if (sameSite != null && !sameSite.isBlank()) b.sameSite(sameSite);
        return b.build();
    }

    // 현재 발급 속성과 맞춰서 필요 경로를 모두 반환
    public ResponseCookie getCleanJwtCookieAtRoot() {
        return deleteCookieAt(jwtCookieName, "/", /*httpOnly*/ false, /*secure*/ false, /*sameSite*/ null);
    }
    public ResponseCookie getCleanJwtCookieAtApi() {
        return deleteCookieAt(jwtCookieName, "/api", false, false, null);
    }

    // (선택) 쿠키 공통 속성
    private static final String COOKIE_PATH = "/"; // 발급과 삭제 모두 동일해야 함
    // private static final String COOKIE_DOMAIN = "example.com"; // 사용한다면 발급/삭제 동일 지정

    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, jwtCookieName);
        return cookie != null ? cookie.getValue() : null;
    }

    public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
        String jwt = generateTokenFromUsername(userPrincipal.getUsername());
        return ResponseCookie.from(jwtCookieName, jwt)
                .path(COOKIE_PATH)
                // 운영에서는 다음을 권장:
                // .httpOnly(true)
                // .secure(true)
                // .sameSite("Lax")
                .httpOnly(false)
                .secure(false)
                .maxAge(Duration.ofMillis(jwtExpirationMs))  // 만료를 토큰과 맞추거나 원하는 기간 지정
                // .domain(COOKIE_DOMAIN)
                .build();
    }

    /** 삭제용 쿠키: name/path(/domain) 동일 + Max-Age=0 */
    public ResponseCookie getCleanJwtCookie() {
        return ResponseCookie.from(jwtCookieName, "")
                .path(COOKIE_PATH)
                // .domain(COOKIE_DOMAIN)
                .httpOnly(false)          // 발급 시와 일관성
                .secure(false)            // 발급 시와 일관성
                .maxAge(0)                // ★ 핵심: 즉시 만료
                .build();
    }

    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key())
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
