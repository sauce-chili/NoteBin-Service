package vstu.isd.notebin.api.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class JwtUtil {
    public static Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder().build().parseClaimsJwt(token).getBody();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    public static String getUserId(Claims claims) {
        if (!claims.containsKey("user_id")) {
            throw new IllegalArgumentException("user_id not found in token");
        }
        return claims.get("user_id", String.class);
    }
}
