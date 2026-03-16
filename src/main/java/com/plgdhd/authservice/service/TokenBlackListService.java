package com.plgdhd.authservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TokenBlackListService {

    private final RedisTemplate<String,String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    @Autowired
    public TokenBlackListService(RedisTemplate<String,String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addToBlackList(String jti, Instant expiresAt){
        long ttlSeconds = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();

        if(ttlSeconds<=0){
            return;
        }

        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "revoked", ttlSeconds, TimeUnit.SECONDS);
        log.debug("addToBlackList jti:{}"+ " ttl:{}",jti, ttlSeconds);
    }

    public boolean isBlackListed(String jti){
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }
}
