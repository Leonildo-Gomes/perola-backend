package cv.perola.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final String jwtSecret;

    @Getter
    private final long accessTokenExpirationSeconds;

    @Getter
    private final long refreshTokenExpirationSeconds;

    public JwtService(
            @Value("${perola.security.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String jwtSecret,
            @Value("${perola.security.jwt.expiration-seconds:86400}") long accessTokenExpirationSeconds,
            @Value("${perola.security.jwt.refresh-expiration-seconds:604800}") long refreshTokenExpirationSeconds
    ) {
        this.jwtSecret = jwtSecret;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        if (jwtSecret != null && jwtSecret.matches("^[0-9a-fA-F]{64}$")) {
            keyBytes = HexFormat.of().parseHex(jwtSecret);
        } else if (jwtSecret != null) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalStateException("Chave secreta JWT não configurada");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UUID userId, String email, List<String> roles, List<TenantMembership> memberships) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (accessTokenExpirationSeconds * 1000L));

        List<Map<String, Object>> membershipMaps = (memberships != null ? memberships : Collections.<TenantMembership>emptyList())
                .stream()
                .map(m -> Map.<String, Object>of(
                        "businessId", m.businessId().toString(),
                        "role", m.role()
                ))
                .toList();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles != null ? roles : Collections.emptyList())
                .claim("memberships", membershipMaps)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    @SuppressWarnings("unchecked")
    public UserPrincipal extractUserPrincipal(String token) {
        Claims claims = extractAllClaims(token);
        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        List<String> roles = claims.get("roles", List.class);
        if (roles == null) {
            roles = Collections.emptyList();
        }

        List<Map<String, Object>> rawMemberships = claims.get("memberships", List.class);
        List<TenantMembership> memberships = Collections.emptyList();
        if (rawMemberships != null) {
            memberships = rawMemberships.stream()
                    .map(m -> new TenantMembership(
                            UUID.fromString(String.valueOf(m.get("businessId"))),
                            String.valueOf(m.get("role"))
                    ))
                    .toList();
        }

        return new UserPrincipal(userId, email, null, roles, memberships, true);
    }
}
