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
import org.apache.commons.lang3.StringUtils;
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
        if (StringUtils.isBlank(req.target())) {
            log.error("(OTP-REQUEST) - Target parameter is null or blank");
            return OtpResponseMessage.rejected(correlationId, OtpErrorCode.INVALID_PHONE, "missing target");
        }
        if (StringUtils.isBlank(req.message()) || !req.message().contains(CODE_REPLACEMENT)) {
            log.error("(OTP-REQUEST) - message parameter is null or blank");
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
            log.info("(OTP-REQUEST) - otp request done successfully - correlationId={}, channel={}, purpose={}, target={}, code={}, challenge={}", correlationId, req.channel(), req.purpose(), req.target(), code, challengeId);
        } else {
            log.info("(OTP-REQUEST) - otp request done successfully - correlationId={}, channel={}, purpose={}, target={}", correlationId, req.channel(), req.purpose(), req.target());
        }
        return OtpResponseMessage.ok(correlationId, signedChallenge, expiresAt);
    }
}
