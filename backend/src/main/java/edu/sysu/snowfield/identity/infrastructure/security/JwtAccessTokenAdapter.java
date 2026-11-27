package edu.sysu.snowfield.identity.infrastructure.security;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import edu.sysu.snowfield.identity.application.AccessTokenPort;

/** 使用 HS256 签发候选契约规定的30分钟 access token；不签发 refresh token。 */
@Component
public class JwtAccessTokenAdapter implements AccessTokenPort {

    private final JwtEncoder encoder;
    private final SnowfieldSecurityProperties properties;

    public JwtAccessTokenAdapter(JwtEncoder encoder, SnowfieldSecurityProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public IssuedAccessToken issue(long userId, Set<String> roles) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId 必须为正整数");
        }
        if (roles == null || roles.isEmpty()
                || roles.stream().anyMatch(role -> !"CUSTOMER".equals(role) && !"ADMIN".equals(role))) {
            throw new IllegalArgumentException("roles 只能包含 CUSTOMER/ADMIN");
        }
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(Long.toString(userId))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("roles", roles.stream().sorted().toList())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
    }
}
