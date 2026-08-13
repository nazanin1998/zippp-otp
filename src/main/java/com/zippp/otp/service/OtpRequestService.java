package com.zippp.otp.service;

import com.zippp.otp.domain.OtpRequest;
import com.zippp.otp.repository.OtpRequestRepository;
import com.zippp.otp.util.OtpCodeGenerator;
import com.zippp.otp.provider.NotificationProvider;
import com.zippp.otp.provider.NotificationRouter;
import com.zippp.otpapi.dto.message.OtpRequestMessage;
import com.zippp.otpapi.dto.message.OtpResponseMessage;
import com.zippp.otpapi.enums.OtpErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

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
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpRequestService {

    private final OtpRequestRepository repository;
    private final OtpCodeGenerator codeGenerator;
    private final NotificationRouter router;

    public OtpResponseMessage handle(String correlationId, OtpRequestMessage req) {
        if (req.target() == null || req.target().isBlank()) {
            return OtpResponseMessage.rejected(
                    correlationId, OtpErrorCode.INVALID_PHONE, "missing target");
        }

        String code = codeGenerator.generate();
        String hash = OtpCodeGenerator.hash(code);
        String challengeKey = UUID.randomUUID().toString();

        OtpRequest challenge = new OtpRequest(
                hash,
                req.channel(),
                req.purpose(),
                Instant.now().plus(req.expiration()),
                0);
        repository.save(challengeKey, challenge, req.expiration());

        NotificationProvider provider = router.resolve(req.channel());
        provider.send(req.target(), code);

        log.info("OTP dispatched corrId={} challengeKey={} channel={} purpose={}",
                correlationId, challengeKey, req.channel(), req.purpose());

        return OtpResponseMessage.ok(correlationId, challengeKey);
    }
}
