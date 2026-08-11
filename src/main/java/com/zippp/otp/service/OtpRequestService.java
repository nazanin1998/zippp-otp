package com.zippp.otp.service;

import com.zippp.otp.domain.OtpChallenge;
import com.zippp.otp.domain.OtpChallengeRepository;
import com.zippp.otp.domain.OtpCodeGenerator;
import com.zippp.otp.provider.NotificationProvider;
import com.zippp.otp.provider.NotificationRouter;
import com.zippp.otpapi.dto.message.OtpRequestMessage;
import com.zippp.otpapi.dto.message.OtpResponseMessage;
import com.zippp.otpapi.enums.OtpErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Core business logic for {@code otp.{purpose}.request} handling.
 *
 * Pure logic — no AMQP annotations. The listener class is the thin adapter.
 *
 * Behavior:
 *   1. Generate a 6-digit code and hash it (SHA-256). Plaintext is held in
 *      memory only long enough to call the provider.
 *   2. Persist the challenge in Redis with a 2-minute TTL.
 *   3. Dispatch via the channel-specific provider.
 *   4. Return an {@link OtpResponseMessage} for the RPC reply.
 *
 * Failure modes:
 *   - Bad target / unknown channel: REJECTED (no retry, no DLQ — bad input)
 *   - Provider throws: ERROR with OTP_NOT_FOUND-equivalent — caller-side retry
 *     is the producer's responsibility; the message is acked so it doesn't
 *     loop in our queue. (Change to NACK if you want broker-side retry.)
 */
@Service
public class OtpRequestService {

    private static final Logger log = LoggerFactory.getLogger(OtpRequestService.class);

    private final OtpChallengeRepository repository;
    private final NotificationRouter router;

    public OtpRequestService(OtpChallengeRepository repository, NotificationRouter router) {
        this.repository = repository;
        this.router = router;
    }

    public OtpResponseMessage handle(OtpRequestMessage req) {
        if (req.challengeKey() == null || req.challengeKey().isBlank()) {
            return OtpResponseMessage.rejected(
                    req.correlationId(), OtpErrorCode.INTERNAL_ERROR, "missing challengeKey");
        }
        if (req.target() == null || req.target().isBlank()) {
            return OtpResponseMessage.rejected(
                    req.correlationId(), OtpErrorCode.INVALID_PHONE, "missing target");
        }

        String code = OtpCodeGenerator.generate();
        String hash = OtpCodeGenerator.hash(code);
        OtpChallenge challenge = new OtpChallenge(
                hash,
                req.channel(),
                req.purpose(),
                Instant.now().plus(OtpChallengeRepository.TTL),
                0);
        repository.save(req.challengeKey(), challenge);

        NotificationProvider provider = router.resolve(req.channel());
        provider.send(req.target(), code);

        log.info("OTP dispatched challengeKey={} channel={} purpose={}",
                req.challengeKey(), req.channel(), req.purpose());

        return OtpResponseMessage.ok(req.correlationId());
    }
}
