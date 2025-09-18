package com.ecommerce.project.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisHealthService {

    private final StringRedisTemplate redis; // AppConfig에서 주입됨

    private static final String TEST_KEY = "health:ping";

    /** 레디스 연결 확인: ping + set/get/del 라운드 트립 */
    public Map<String, Object> check() {
        long t0 = System.nanoTime();

        // 1) 저수준 PING
        String pingReply = doPing();

        // 2) set/get/del 라운드 트립
        String payload = "pong@" + Instant.now();
        redis.opsForValue().set(TEST_KEY, payload);
        String readBack = redis.opsForValue().get(TEST_KEY);
        Boolean deleted = redis.delete(TEST_KEY);

        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;

        Map<String, Object> result = new HashMap<>();
        result.put("ok", "PONG".equalsIgnoreCase(pingReply) && payload.equals(readBack));
        result.put("redisPing", pingReply);                // 기대: "PONG"
        result.put("writeReadOK", payload.equals(readBack));
        result.put("deleted", Boolean.TRUE.equals(deleted));
        result.put("roundTripMillis", elapsedMs);
        result.put("readValue", readBack);
        return result;
    }

    private String doPing() {
        try {
            return redis.execute(RedisConnection::ping);
        } catch (DataAccessException e) {
            return "PING_FAILED: " + e.getMessage();
        }
    }
}
