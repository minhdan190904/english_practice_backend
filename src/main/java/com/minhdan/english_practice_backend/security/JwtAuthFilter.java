package com.minhdan.english_practice_backend.security;

import com.minhdan.english_practice_backend.entity.User;
import com.minhdan.english_practice_backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT authentication filter — replaces FirebaseTokenFilter.
 * Validates app-issued RS256 JWT from Authorization header.
 * Extremely fast — no network calls, just RSA signature verification.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtService.isTokenValid(token)) {
                    Claims claims = jwtService.parseToken(token);

                    // Only process ACCESS tokens (not REFRESH)
                    String tokenType = claims.get("type", String.class);
                    if ("ACCESS".equals(tokenType)) {
                        String userId = claims.getSubject();
                        String role = claims.get("role", String.class);

                        User user = userRepository.findById(userId).orElse(null);
                        if (user != null) {
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            user, null,
                                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("JWT auth failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip filter for public endpoints
        return path.equals("/api/v1/user/register")
                || path.equals("/api/v1/user/register-device")
                || path.equals("/api/v1/user/refresh")
                || path.startsWith("/api/v1/public/");
    }
}
