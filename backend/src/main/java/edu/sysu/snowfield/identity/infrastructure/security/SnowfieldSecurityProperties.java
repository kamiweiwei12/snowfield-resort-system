package edu.sysu.snowfield.identity.infrastructure.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("snowfield.security")
public record SnowfieldSecurityProperties(
        String jwtSecret,
        String issuer,
        Duration accessTokenTtl,
        String allowedOrigin) {
}
