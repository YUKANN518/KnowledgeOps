package com.knowledgeops.auth.application;

import com.knowledgeops.auth.infrastructure.AuthProperties;
import com.knowledgeops.user.domain.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final AuthProperties properties;
  private final Clock clock;

  public JwtService(AuthProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
    var key =
        new SecretKeySpec(properties.jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    this.encoder = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
    var d = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    var timestampValidator = new JwtTimestampValidator();
    timestampValidator.setClock(clock);
    d.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            timestampValidator, new JwtIssuerValidator(properties.issuer())));
    this.decoder = d;
  }

  public String createAccessToken(User user, UUID sessionId) {
    Instant now = clock.instant();
    List<String> roles = user.getRoles().stream().map(r -> r.getCode().name()).sorted().toList();
    var claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plus(properties.accessTokenTtl()))
            .id(UUID.randomUUID().toString())
            .claim("roles", roles)
            .claim("sid", sessionId.toString())
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  public Jwt decode(String token) {
    return decoder.decode(token);
  }
}
