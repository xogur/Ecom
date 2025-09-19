package com.ecommerce.project.service.like;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RedisLikeService {

    private final StringRedisTemplate redis;

    private String usersKey(long productId) { return "like:prod:" + productId + ":users"; }
    private String countKey(long productId) { return "like:prod:" + productId + ":count"; }
    private String userKey(String userEmail) { return userEmail == null ? "" : userEmail.toLowerCase(Locale.ROOT); }

    /** 좋아요 토글: 집합에 사용자를 추가/삭제하고, SCARD로 재계산한 값을 캐시에 반영 */
    public LikeResult toggle(String userEmail, long productId) {
        final String uk = usersKey(productId);
        final String ck = countKey(productId);
        final String u  = userKey(userEmail);

        // 이미 좋아요면 제거, 아니면 추가
        boolean liked;
        Long removed = redis.opsForSet().remove(uk, u);
        if (removed != null && removed > 0) {
            liked = false;
        } else {
            redis.opsForSet().add(uk, u);
            liked = true;
        }

        // 현재 집합 크기로 정확한 카운트 계산 후 캐시에 반영
        Long scard = redis.opsForSet().size(uk);
        long count = (scard == null) ? 0L : scard;
        redis.opsForValue().set(ck, Long.toString(count));

        return new LikeResult(liked, count);
    }

    /** 현재 사용자 좋아요 여부 */
    public boolean isLiked(String userEmail, long productId) {
        Boolean yes = redis.opsForSet().isMember(usersKey(productId), userKey(userEmail));
        return Boolean.TRUE.equals(yes);
    }

    /** 현재 상품 좋아요 수 (캐시값 우선, 없으면 SCARD 후 보정) */
    public long getCount(long productId) {
        String v = redis.opsForValue().get(countKey(productId));
        if (v != null) {
            try { return Long.parseLong(v); } catch (NumberFormatException ignore) {}
        }
        Long scard = redis.opsForSet().size(usersKey(productId));
        long c = (scard == null) ? 0L : scard;
        redis.opsForValue().set(countKey(productId), Long.toString(c));
        return c;
    }

    /** 다건 카운트 조회 */
    public Map<Long, Long> getCounts(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();

        // 1) 캐시 조회
        List<String> keys = productIds.stream().map(this::countKey).toList();
        List<String> vals = redis.opsForValue().multiGet(keys);

        Map<Long, Long> out = new LinkedHashMap<>();
        int i = 0;
        for (Long pid : productIds) {
            String v = (vals != null && i < vals.size()) ? vals.get(i) : null;
            long c;
            if (v != null) {
                try { c = Long.parseLong(v); } catch (NumberFormatException e) { c = 0; }
            } else {
                // 2) 캐시 미스면 SCARD로 보정 후 캐시 업데이트
                Long scard = redis.opsForSet().size(usersKey(pid));
                c = (scard == null) ? 0L : scard;
                redis.opsForValue().set(countKey(pid), Long.toString(c));
            }
            out.put(pid, c);
            i++;
        }
        return out;
    }

    /** 현재 사용자 기준 다건 좋아요여부 */
    public Map<Long, Boolean> getLikedFlags(String userEmail, Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();
        String u = userKey(userEmail);
        Map<Long, Boolean> out = new LinkedHashMap<>();
        for (Long pid : productIds) {
            Boolean yes = redis.opsForSet().isMember(usersKey(pid), u);
            out.put(pid, Boolean.TRUE.equals(yes));
        }
        return out;
    }

    public record LikeResult(boolean liked, long count) {}
}
