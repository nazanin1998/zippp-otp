package com.zippp.otp.service;

import com.zippp.otp.domain.OtpChallenge;
import com.zippp.otp.domain.OtpChallengeRepository;
import com.zippp.otp.domain.OtpCodeGenerator;
import com.zippp.otpapi.dto.message.OtpResponseMessage;
import com.zippp.otpapi.dto.request.OtpVerifyRequest;
import com.zippp.otpapi.enums.OtpErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Core business logic for {@code otp.{purpose}.verify} handling.
 *
 * Behavior:
 *   1. Look up the challenge in Redis.
 *   2. Increment attempts atomically (RMW).
 *   3. Compare SHA-256(submitted) with stored hash.
 *   4. On match: delete the challenge and return OK.
 *   5. On mismatch / expiry / exhaustion: return the appropriate error code.
 *
 * Errors are reported as ERROR status so the producer can surface them to the
 * end user. The message is acked either way — retrying the same OTP code is
 * pointless and would loop forever.
 */
@Service
public class OtpVerifyService {

    private static final Logger log = LoggerFactory.getLogger(OtpVerifyService.class);

    private final OtpChallengeRepository repository;

    public OtpVerifyService(OtpChallengeRepository repository) {
        this.repository = repository;
    }

    public OtpResponseMessage handle(OtpVerifyRequest req) {
        if (req.challengeKey() == null || req.challengeKey().isBlank()) {
            return OtpResponseMessage.rejected(
                    req.correlationId(), OtpErrorCode.INTERNAL_ERROR, "missing challengeKey");
        }
        if (req.otp() == null || req.otp().isBlank()) {
            return OtpResponseMessage.rejected(
                    req.correlationId(), OtpErrorCode.OTP_MISMATCH, "missing otp");
        }

        Optional<OtpChallenge> maybe = repository.find(req.challengeKey());
        if (maybe.isEmpty()) {
            return OtpResponseMessage.error(
                    req.correlationId(), OtpErrorCode.OTP_NOT_FOUND,
                    "no active challenge for this key");
        }

        OtpChallenge current = maybe.get();
        Instant now = Instant.now();

        if (current.isExpired(now)) {
            repository.delete(req.challengeKey());
            return OtpResponseMessage.error(
                    req.correlationId(), OtpErrorCode.OTP_EXPIRED, "challenge expired");
        }

        // increment attempts FIRST so a malicious concurrent verify can't
        // bypass the attempts cap by reading after match succeeds.
        Optional<OtpChallenge> afterRmw = repository.incrementAttempts(req.challengeKey());
        if (afterRmw.isEmpty()) {
            // Disappeared between read and write — race with TTL.
            return OtpResponseMessage.error(
                    req.correlationId(), OtpErrorCode.OTP_EXPIRED, "challenge expired");
        }
        OtpChallenge updated = afterRmw.get();

        if (updated.isExhausted()) {
            repository.delete(req.challengeKey());
            return OtpResponseMessage.error(
                    req.correlationId(), OtpErrorCode.OTP_TOO_MANY_ATTEMPTS,
                    "too many attempts");
        }

        String submittedHash = OtpCodeGenerator.hash(req.otp());
        if (!constantTimeEquals(submittedHash, current.codeHash())) {
            return OtpResponseMessage.error(
                    req.correlationId(), OtpErrorCode.OTP_MISMATCH, "code does not match");
        }

        // Success — burn the challenge so it can't be reused.
        repository.delete(req.challengeKey());
        log.info("OTP verified challengeKey={}", req.challengeKey());
        return OtpResponseMessage.ok(req.correlationId());
    }

    /** Constant-time comparison to avoid leaking match length via timing. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
