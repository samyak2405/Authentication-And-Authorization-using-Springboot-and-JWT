package com.javaproject.application.service.otp;

import com.javaproject.application.model.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service("redisOtpStore")
public class RedisOtpStore implements OtpStore {

    private static final String VERIFY_SCRIPT = """
            local key = KEYS[1]
            local providedHash = ARGV[1]
            local nowEpoch = tonumber(ARGV[2])
            local maxAttempts = tonumber(ARGV[3])
            local consumedTtlSeconds = tonumber(ARGV[4])

            if redis.call('EXISTS', key) == 0 then
              return {'NOT_FOUND', '0'}
            end

            local consumed = redis.call('HGET', key, 'consumed')
            if consumed == '1' then
              return {'NOT_FOUND', '0'}
            end

            local expiresAt = tonumber(redis.call('HGET', key, 'expiresAt') or '0')
            local attempts = tonumber(redis.call('HGET', key, 'attempts') or '0')
            if expiresAt <= nowEpoch then
              return {'EXPIRED', tostring(attempts)}
            end

            local expectedHash = redis.call('HGET', key, 'tokenHash') or ''
            attempts = attempts + 1

            if expectedHash == providedHash then
              redis.call('HSET', key, 'attempts', tostring(attempts), 'consumed', '1', 'consumedAt', tostring(nowEpoch))
              redis.call('EXPIRE', key, consumedTtlSeconds)
              return {'VERIFIED', tostring(attempts)}
            end

            if attempts >= maxAttempts then
              redis.call('HSET', key, 'attempts', tostring(attempts), 'consumed', '1', 'consumedAt', tostring(nowEpoch))
              redis.call('EXPIRE', key, consumedTtlSeconds)
              return {'ATTEMPTS_EXCEEDED', tostring(attempts)}
            end

            redis.call('HSET', key, 'attempts', tostring(attempts))
            return {'INVALID', tostring(attempts)}
            """;

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<List> verifyRedisScript;

    public RedisOtpStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.verifyRedisScript = new DefaultRedisScript<>();
        this.verifyRedisScript.setScriptText(VERIFY_SCRIPT);
        this.verifyRedisScript.setResultType(List.class);
    }

    @Override
    public OtpIssueResult issue(OtpIssueRequest request) {
        String key = stateKey(request.getUser(), request.getPurpose());
        if (request.isInvalidateExistingTokens()) {
            stringRedisTemplate.delete(key);
        }

        long nowEpoch = request.getIssuedAt().toEpochSecond();
        long expiresAtEpoch = request.getExpiresAt().toEpochSecond();
        long resendAfterEpoch = request.getIssuedAt().plusSeconds(request.getResendCooldownSeconds()).toEpochSecond();
        stringRedisTemplate.opsForHash().putAll(key, Map.of(
                "tokenHash", request.getTokenHash(),
                "issuedAt", String.valueOf(nowEpoch),
                "expiresAt", String.valueOf(expiresAtEpoch),
                "resendAfter", String.valueOf(resendAfterEpoch),
                "attempts", "0",
                "consumed", "0",
                "ipAddress", request.getIpAddress() == null ? "" : request.getIpAddress(),
                "deliveryChannel", request.getDeliveryChannel() == null ? "" : request.getDeliveryChannel()
        ));
        stringRedisTemplate.expireAt(key, java.time.Instant.ofEpochSecond(expiresAtEpoch));

        return OtpIssueResult.builder()
                .issuedAt(request.getIssuedAt())
                .expiresAt(request.getExpiresAt())
                .build();
    }

    @Override
    public OtpResendCheckResult checkResendAllowed(OtpResendCheckRequest request) {
        String key = stateKey(request.getUser(), request.getPurpose());
        Object resendAfterObj = stringRedisTemplate.opsForHash().get(key, "resendAfter");
        Object consumedObj = stringRedisTemplate.opsForHash().get(key, "consumed");
        if (resendAfterObj == null || "1".equals(String.valueOf(consumedObj))) {
            return OtpResendCheckResult.builder().resendAllowed(true).waitSeconds(0).build();
        }

        long nowEpoch = request.getNow().toEpochSecond();
        long resendAfterEpoch = Long.parseLong(String.valueOf(resendAfterObj));
        if (resendAfterEpoch > nowEpoch) {
            return OtpResendCheckResult.builder()
                    .resendAllowed(false)
                    .waitSeconds(resendAfterEpoch - nowEpoch)
                    .build();
        }

        return OtpResendCheckResult.builder().resendAllowed(true).waitSeconds(0).build();
    }

    @Override
    public OtpVerifyResult verify(OtpVerifyRequest request) {
        String key = stateKey(request.getUser(), request.getPurpose());
        List result = stringRedisTemplate.execute(
                verifyRedisScript,
                Collections.singletonList(key),
                request.getProvidedTokenHash(),
                String.valueOf(request.getNow().toEpochSecond()),
                String.valueOf(request.getMaxAttempts()),
                "60"
        );

        if (result == null || result.size() < 2) {
            return OtpVerifyResult.builder().status(OtpVerifyStatus.NOT_FOUND).attempts(0).build();
        }

        OtpVerifyStatus status = OtpVerifyStatus.valueOf(String.valueOf(result.get(0)));
        int attempts = Integer.parseInt(String.valueOf(result.get(1)));
        return OtpVerifyResult.builder().status(status).attempts(attempts).build();
    }

    @Override
    public void invalidateActive(User user, String purpose, OffsetDateTime now) {
        stringRedisTemplate.delete(stateKey(user, purpose));
    }

    private String stateKey(User user, String purpose) {
        return "otp:" + purpose + ":" + user.getId() + ":state";
    }
}
