package com.knowledgeops.auth.application;

import com.knowledgeops.audit.application.AuditService;
import com.knowledgeops.audit.domain.AuditAction;
import com.knowledgeops.auth.infrastructure.AuthProperties;
import com.knowledgeops.shared.domain.*;
import com.knowledgeops.user.domain.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  public record Tokens(
      String accessToken,
      long expiresIn,
      String refreshToken,
      String csrfToken,
      User user,
      UUID sessionId) {}

  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final JwtService jwt;
  private final RefreshTokenService refresh;
  private final SessionRegistry sessions;
  private final AuthProperties props;
  private final AuditService audit;

  public AuthService(
      UserRepository users,
      PasswordEncoder passwords,
      JwtService jwt,
      RefreshTokenService refresh,
      SessionRegistry sessions,
      AuthProperties props,
      AuditService audit) {
    this.users = users;
    this.passwords = passwords;
    this.jwt = jwt;
    this.refresh = refresh;
    this.sessions = sessions;
    this.props = props;
    this.audit = audit;
  }

  public Tokens login(String email, String password) {
    String normalized = normalize(email);
    Optional<User> found = users.findByEmail(normalized);
    if (found.isEmpty()
        || !passwords.matches(password, found.get().getPasswordHash())
        || found.get().getStatus() != UserStatus.ACTIVE) {
      audit.record(
          "ANONYMOUS",
          null,
          AuditAction.LOGIN_FAILURE,
          "USER",
          null,
          Map.of(
              "emailHash",
              Integer.toHexString(normalized.hashCode()),
              "reason",
              "INVALID_CREDENTIALS"));
      throw invalid();
    }
    User user = found.get();
    RefreshTokenService.Issued issued = refresh.issue(user);
    try {
      sessions.activate(issued.familyId(), props.refreshTokenTtl());
    } catch (RuntimeException ex) {
      refresh.revokeFamily(issued.familyId());
      throw ex;
    }
    audit.record(
        "USER",
        user.getId(),
        AuditAction.LOGIN_SUCCESS,
        "USER",
        user.getId().toString(),
        Map.of("result", "SUCCESS"));
    return tokens(user, issued);
  }

  public Tokens refresh(String raw) {
    var rotation = refresh.rotate(raw);
    if (rotation.status() != RefreshTokenService.RotationStatus.OK) {
      if (rotation.familyId() != null) sessions.revoke(rotation.familyId());
      throw new BusinessException(
          ErrorCode.TOKEN_REVOKED, HttpStatus.UNAUTHORIZED, "Refresh token is invalid or revoked");
    }
    try {
      sessions.activate(rotation.familyId(), props.refreshTokenTtl());
    } catch (RuntimeException ex) {
      refresh.revokeFamily(rotation.familyId());
      throw ex;
    }
    return tokens(rotation.user(), rotation.issued());
  }

  public void logout(String raw, UUID actorId) {
    UUID family = refresh.revoke(raw, actorId);
    if (family != null) sessions.revoke(family);
  }

  private Tokens tokens(User user, RefreshTokenService.Issued issued) {
    return new Tokens(
        jwt.createAccessToken(user, issued.familyId()),
        props.accessTokenTtl().toSeconds(),
        issued.raw(),
        randomCsrf(),
        user,
        issued.familyId());
  }

  private String randomCsrf() {
    byte[] b = new byte[24];
    new java.security.SecureRandom().nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  private BusinessException invalid() {
    return new BusinessException(
        ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED, "Invalid email or password");
  }

  private String normalize(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }
}
