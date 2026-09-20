package com.knowledgeops.auth.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("knowledgeops.auth")
public record AuthProperties(
    String jwtSecret,
    String issuer,
    Duration accessTokenTtl,
    Duration refreshTokenTtl,
    boolean refreshCookieSecure,
    String allowedOrigin) {
  public AuthProperties {
    if (jwtSecret == null
        || jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32)
      throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
  }
}
