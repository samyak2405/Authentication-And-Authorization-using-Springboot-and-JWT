package com.javaproject.application.repository;

import com.javaproject.application.model.OtpToken;
import com.javaproject.application.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpToken> findFirstByUserAndPurposeAndConsumedAtIsNullOrderByIssuedAtDesc(User user, String purpose);

    List<OtpToken> findByUserAndPurposeAndConsumedAtIsNull(User user, String purpose);

    long deleteByExpiresAtBeforeOrConsumedAtBefore(OffsetDateTime expiresAt, OffsetDateTime consumedAt);
}
