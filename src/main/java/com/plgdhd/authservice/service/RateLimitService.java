package com.plgdhd.authservice.service;

import com.plgdhd.authservice.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Value("${rate-limit.login.max-attempts}")
    private int maxAttempts;

    @Value("${rate-limit.login.window-seconds}")
    private long windowSeconds;

    @Value("${rate-limit.login.block-seconds}")
    private long blockSeconds;

    private static final String ATTEMPTS_PREFIX = "rate_limit:login:";
    private static final String BLOCKED_PREFIX  = "rate_limit:blocked:";

    public void checkLoginRateLimit(String ipAddress) {
        String blockedKey = BLOCKED_PREFIX + ipAddress;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blockedKey))) {
            Long ttl = redisTemplate.getExpire(blockedKey);
            long retryAfter = ttl != null && ttl > 0 ? ttl : blockSeconds;
            log.warn("Заблокированный IP {} пытается войти, повторите через: {}s", ipAddress, retryAfter);
            throw new RateLimitException(retryAfter);
        }
    }

    public void recordFailedAttempt(String ipAddress) {
        String attemptsKey = ATTEMPTS_PREFIX + ipAddress;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts == null) return;

        if (attempts == 1) {
            redisTemplate.expire(attemptsKey, Duration.ofSeconds(windowSeconds));
        }

        log.debug("Неудачный вход с IP {}: {}/{}", ipAddress, attempts, maxAttempts);

        if (attempts >= maxAttempts) {
            redisTemplate.opsForValue()
                    .set(BLOCKED_PREFIX + ipAddress, "blocked", Duration.ofSeconds(blockSeconds));
            redisTemplate.delete(attemptsKey);
            log.warn("IP {} заблокирован на {}s после {} попыток", ipAddress, blockSeconds, attempts);
        }
    }

    public void resetAttempts(String ipAddress) {
        redisTemplate.delete(ATTEMPTS_PREFIX + ipAddress);
    }


}
