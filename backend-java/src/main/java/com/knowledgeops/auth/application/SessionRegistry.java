package com.knowledgeops.auth.application;

import com.knowledgeops.auth.infrastructure.*;
import com.knowledgeops.shared.domain.*;
import java.time.*;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SessionRegistry {
  private final StringRedisTemplate redis;
  private final RefreshTokenJpaRepository tokens;
  private final Clock clock;

  public SessionRegistry(StringRedisTemplate redis, RefreshTokenJpaRepository tokens, Clock clock) {
    this.redis = redis;
    this.tokens = tokens;
    this.clock = clock;
  }

  public void activate(UUID sid, Duration ttl) {
    execute(() -> redis.opsForValue().set(key(sid), "ACTIVE", ttl));
  }

  public void revoke(UUID sid) {
    execute(() -> redis.delete(key(sid)));
  }

  public boolean isActive(UUID sid, UUID userId) {
    try {
      return "ACTIVE".equals(redis.opsForValue().get(key(sid)))
          && tokens.existsByFamilyIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(
              sid, userId, clock.instant());
    } catch (DataAccessException ex) {
      throw unavailable();
    }
  }

  private void execute(Runnable action) {
    try {
      action.run();
    } catch (DataAccessException ex) {
      throw unavailable();
    }
  }

  private BusinessException unavailable() {
    return new BusinessException(
        ErrorCode.AUTH_DEPENDENCY_UNAVAILABLE,
        HttpStatus.SERVICE_UNAVAILABLE,
        "Authentication dependency unavailable");
  }

  private String key(UUID sid) {
    return "session:" + sid;
  }
}
