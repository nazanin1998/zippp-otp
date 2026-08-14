package com.zippp.otp.service;

import com.zippp.otp.config.properties.OtpProperties;
import com.zippp.otp.domain.OtpRequest;
import com.zippp.otp.repository.OtpRequestRepository;
import com.zippp.otp.util.OtpCodeGenerator;
import com.zippp.otp.provider.NotificationProvider;
import com.zippp.otp.provider.NotificationRouter;
import com.zippp.otpapi.dto.message.OtpRequestMessage;
import com.zippp.otpapi.dto.message.OtpResponseMessage;
import com.zippp.otpapi.enums.OtpErrorCode;
import com.zippp.signature.service.JwtSigner;
import com.zippp.signature.service.SaltedHash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class OtpRequestService {

    private static final String CODE_REPLACEMENT = "{code}";
    private final OtpRequestRepository repository;
    private final OtpCodeGenerator codeGenerator;
    private final NotificationRouter router;
    private final OtpProperties properties;
    private final JwtSigner jwtSigner;

    public OtpResponseMessage handle(String correlationId, OtpRequestMessage req) {
        if (req.target() == null || req.target().isBlank()) {
            return OtpResponseMessage.rejected(
                    correlationId, OtpErrorCode.INVALID_PHONE, "missing target");
        }
        if (req.message() == null || req.message().isBlank() || !req.message().contains(CODE_REPLACEMENT)) {
            return OtpResponseMessage.rejected(
                    correlationId, OtpErrorCode.INVALID_MESSAGE, "missing message");
        }
        String code = codeGenerator.generate();
        String hash = SaltedHash.concatenatedSaltAndHash(code);
        String message = req.message().replace(CODE_REPLACEMENT, code);
        String challengeId = UUID.randomUUID().toString();
        String signedChallenge =  jwtSigner.sign(req.target(), challengeId, req.expiration());
        Instant expiresAt = Instant.now().plus(req.expiration());

        OtpRequest otpRequest = new OtpRequest(
                hash,
                req.channel(),
                req.purpose(),
                expiresAt,
                0);
        repository.save(challengeId, otpRequest, req.expiration());

        NotificationProvider provider = router.resolve(req.channel());
        provider.send(req.target(), message);

        if (properties.isTestLog()) {
            log.debug("(TEST-LOG) - request - corrId={} challengeKey={} channel={} purpose={}, target={}, code={}",
                    correlationId, challengeId, req.channel(), req.purpose(), req.target(), code);
        }
        log.info("OTP dispatched corrId={} challengeKey={} channel={} purpose={}, target={}",
                correlationId, challengeId, req.channel(), req.purpose(), req.target());

        return OtpResponseMessage.ok(correlationId, signedChallenge, expiresAt);
    }
}
