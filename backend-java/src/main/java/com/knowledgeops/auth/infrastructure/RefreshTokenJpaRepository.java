package com.knowledgeops.auth.infrastructure;

import com.knowledgeops.auth.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select t from RefreshToken t join fetch t.user u join fetch u.roles r join fetch r.permissions where t.tokenHash=:hash")
  Optional<RefreshToken> findLockedByHash(@Param("hash") String hash);

  @Modifying
  @Query(
      "update RefreshToken t set t.revokedAt=:now where t.familyId=:familyId and t.revokedAt is null")
  int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

  boolean existsByFamilyIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(
      UUID familyId, UUID userId, Instant now);
}
