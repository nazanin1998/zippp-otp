package com.zippp.otp.service;

import com.zippp.otp.domain.OtpPassed;
import com.zippp.otp.repository.OtpPassedRepository;
import com.zippp.otp.repository.OtpRequestRepository;
import com.zippp.otpapi.dto.message.OtpVerifyRequestMessage;
import com.zippp.otpapi.dto.message.OtpVerifyResponseMessage;
import com.zippp.otpapi.enums.OtpErrorCode;
import com.zippp.otpapi.enums.OtpPurpose;
import com.zippp.signature.dto.ParsedJwtDto;
import com.zippp.signature.service.JwtParser;
import com.zippp.signature.service.JwtSigner;
import com.zippp.signature.service.SaltedHash;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class OtpVerifyService {

    private final OtpRequestRepository requestRepository;
    private final OtpPassedRepository passedRepository;
    private final JwtParser jwtParser;
    private final JwtSigner jwtSigner;

    public OtpVerifyResponseMessage handle(String correlationId, OtpVerifyRequestMessage req) {
        if (StringUtils.isEmpty(req.signedChallenge())) {
            return OtpVerifyResponseMessage.rejected(
                    OtpErrorCode.INVALID_CHALLENGE, "missing challengeKey", correlationId);
        }
        if (StringUtils.isEmpty(req.otp())) {
            return OtpVerifyResponseMessage.rejected(
                    OtpErrorCode.OTP_MISMATCH, "missing otp", correlationId);
        }

        final OtpPurpose purpose = req.purpose();
        final Duration verifyExpiration = req.expiration();

        final ParsedJwtDto parsed = jwtParser.parseAndGetFirstValue(req.signedChallenge());
        final String target = parsed.getUser();
        final String requestChallenge = parsed.getValue();

        return requestRepository.find(requestChallenge, purpose)
                .map(otpRequest -> {
                    if (otpRequest.isExpired(Instant.now())) {
                        requestRepository.delete(requestChallenge, purpose);
                        return OtpVerifyResponseMessage.verifyFailed(
                                OtpErrorCode.OTP_EXPIRED, "challenge expired", correlationId);
                    }
                    if (!SaltedHash.matches(req.otp(), otpRequest.codeHash())) {
                        return OtpVerifyResponseMessage.verifyFailed(
                                OtpErrorCode.OTP_MISMATCH, "code does not match", correlationId);
                    }
                    requestRepository.delete(requestChallenge, purpose);

                    return saveOtpPassedAndGetResponse(target, correlationId, verifyExpiration, purpose);
                })
                .orElse(OtpVerifyResponseMessage.verifyFailed(
                                OtpErrorCode.OTP_NOT_FOUND, "no active challenge for this key", correlationId));
    }

    private OtpVerifyResponseMessage saveOtpPassedAndGetResponse(
            String target,
            String correlationId,
            Duration verifyExpiration,
            OtpPurpose purpose
    ) {
        final String verifyChallenge = UUID.randomUUID().toString();
        final String verifySignedChallenge = jwtSigner.sign(target, verifyChallenge, verifyExpiration);
        final Instant verifyExpiresAt = Instant.now().plus(verifyExpiration);

        OtpPassed otpPassed = new OtpPassed(purpose, verifyExpiresAt);
        passedRepository.save(verifyChallenge, otpPassed, verifyExpiration);

        log.info("OTP verified challengeKey={}, target={}", verifyChallenge, target);
        return OtpVerifyResponseMessage.ok(verifySignedChallenge, verifyExpiresAt, correlationId);
    }
}
