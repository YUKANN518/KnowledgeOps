package com.knowledgeops.auth.application;

import com.knowledgeops.audit.application.AuditService;
import com.knowledgeops.audit.domain.AuditAction;
import com.knowledgeops.auth.domain.RefreshToken;
import com.knowledgeops.auth.infrastructure.*;
import com.knowledgeops.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {
  public enum RotationStatus {
    OK,
    INVALID,
    REPLAYED
  }

  public record Issued(String raw, UUID familyId, Instant expiresAt) {}

  public record Rotation(RotationStatus status, User user, Issued issued, UUID familyId) {}

  private final RefreshTokenJpaRepository repository;
  private final AuditService audit;
  private final Clock clock;
  private final AuthProperties props;
  private final SecureRandom random = new SecureRandom();

  public RefreshTokenService(
      RefreshTokenJpaRepository repository, AuditService audit, Clock clock, AuthProperties props) {
    this.repository = repository;
    this.audit = audit;
    this.clock = clock;
    this.props = props;
  }

  @Transactional
  public Issued issue(User user) {
    UUID family = UUID.randomUUID();
    return persist(user, family);
  }

  @Transactional
  public Rotation rotate(String raw) {
    Instant now = clock.instant();
    Optional<RefreshToken> found = repository.findLockedByHash(hash(raw));
    if (found.isEmpty()) return new Rotation(RotationStatus.INVALID, null, null, null);
    RefreshToken old = found.get();
    UUID family = old.getFamilyId();
    if (old.getConsumedAt() != null || old.getRevokedAt() != null) {
      repository.revokeFamily(family, now);
      audit.record(
          "USER",
          old.getUser().getId(),
          AuditAction.REFRESH_REPLAY,
          "REFRESH_SESSION",
          family.toString(),
          Map.of("result", "REPLAYED"));
      return new Rotation(RotationStatus.REPLAYED, null, null, family);
    }
    if (!old.getExpiresAt().isAfter(now)) {
      old.revoke(now);
      return new Rotation(RotationStatus.INVALID, null, null, family);
    }
    old.consume(now);
    Issued next = persist(old.getUser(), family);
    audit.record(
        "USER",
        old.getUser().getId(),
        AuditAction.REFRESH,
        "REFRESH_SESSION",
        family.toString(),
        Map.of("result", "ROTATED"));
    return new Rotation(RotationStatus.OK, old.getUser(), next, family);
  }

  @Transactional
  public UUID revoke(String raw, UUID actorId) {
    Optional<RefreshToken> found = repository.findLockedByHash(hash(raw));
    if (found.isEmpty()) return null;
    UUID family = found.get().getFamilyId();
    repository.revokeFamily(family, clock.instant());
    audit.record(
        "USER",
        actorId,
        AuditAction.LOGOUT,
        "REFRESH_SESSION",
        family.toString(),
        Map.of("result", "REVOKED"));
    return family;
  }

  @Transactional
  public void revokeFamily(UUID family) {
    repository.revokeFamily(family, clock.instant());
  }

  private Issued persist(User user, UUID family) {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Instant expires = clock.instant().plus(props.refreshTokenTtl());
    repository.save(
        new RefreshToken(UUID.randomUUID(), user, family, hash(raw), expires, clock.instant()));
    return new Issued(raw, family, expires);
  }

  private String hash(String raw) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
