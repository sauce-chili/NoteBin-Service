package vstu.isd.notebin.controller.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vstu.isd.notebin.api.auth.AuthApi;
import vstu.isd.notebin.api.auth.VerifyAccessTokenRequest;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthApi authApi;
    private final String userIdHeaderAttribute;

    private final static String TOKEN_PREFIX = "Bearer ";

    private final static String USER_ID_TOKEN_KEY = "user_id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            nextFilter(request, response, filterChain);
        }

        String userId = verifyTokenAndGetUserId(token);
        boolean isTokenValid = userId != null;

        if (isTokenValid) {
            request.setAttribute(userIdHeaderAttribute, userId);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        nextFilter(request, response, filterChain);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            return null;
        }
        return header.substring(TOKEN_PREFIX.length());
    }

    private void nextFilter(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        filterChain.doFilter(request, response);
    }

    private String verifyTokenAndGetUserId(String token) {
        try {
            Claims claims = getClaims(token); // also throw exception if token is expired or in invalid format

            if (!claimsContainsMandatoryFields(claims)) {
                return null;
            }

            boolean isTokenValid = authApi.verifyAccessToken(new VerifyAccessTokenRequest(token));

            return isTokenValid ? getUserId(claims) : null;
        } catch (Exception e) {
            log.error("Error while verifying token", e);
            return null;
        }
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder().build().parseClaimsJwt(token).getBody();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    public static boolean claimsContainsMandatoryFields(Claims claims) {
        return claims.containsKey(USER_ID_TOKEN_KEY) &&
                claims.getSubject() != null && // login
                claims.getIssuedAt() != null;
    }

    public static String getUserId(Claims claims) {
        if (!claims.containsKey(USER_ID_TOKEN_KEY)) {
            throw new IllegalArgumentException("user_id is not present in token");
        }
        return claims.get(USER_ID_TOKEN_KEY, String.class);
    }
}
