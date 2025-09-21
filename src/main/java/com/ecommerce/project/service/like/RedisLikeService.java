package com.ecommerce.project.service.like;

import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.QProduct;
import com.ecommerce.project.payload.ProductCardDTO;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisLikeService {

    private final StringRedisTemplate redis;

    private final JPAQueryFactory queryFactory;

    // 상품 → 사용자 집합
    private String usersKey(long productId) { return "like:prod:" + productId + ":users"; }
    // 상품별 카운트 캐시
    private String countKey(long productId) { return "like:prod:" + productId + ":count"; }
    // 사용자 → 상품 집합(역인덱스) : "내가 좋아요한 상품들"
    private String userProductsKey(String userEmail) { return "like:user:" + userKey(userEmail) + ":products"; }

    /** 이메일 정규화 (null/blank 방지) */
    private String userKey(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("userEmail must not be null/blank");
        }
        return userEmail.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 좋아요 토글
     * - 상품→사용자 집합 갱신
     * - 사용자→상품 집합(역인덱스)도 함께 갱신
     * - 정확도를 위해 SCARD로 카운트 재계산 후 캐시에 반영
     */
    public LikeResult toggle(String userEmail, long productId) {
        final String u   = userKey(userEmail);
        final String uk  = usersKey(productId);
        final String ck  = countKey(productId);
        final String upk = userProductsKey(userEmail);

        boolean liked;
        // 현재 상태 확인
        Boolean already = redis.opsForSet().isMember(uk, u);
        if (Boolean.TRUE.equals(already)) {
            // 좋아요 해제
            redis.opsForSet().remove(uk, u);
            redis.opsForSet().remove(upk, String.valueOf(productId));
            liked = false;
        } else {
            // 좋아요 추가
            redis.opsForSet().add(uk, u);
            redis.opsForSet().add(upk, String.valueOf(productId));
            liked = true;
        }

        // 집합 크기로 카운트 보정 + 캐시 반영
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

    /** 현재 상품 좋아요 수 (캐시 우선, 없으면 SCARD 후 캐시 갱신) */
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

        Map<Long, Long> out = new LinkedHashMap<>();
        for (Long pid : productIds) {
            out.put(pid, getCount(pid)); // 캐시 미스 시 SCARD로 보정
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

    // --------------------------
    // ✅ 프로필용: 내가 좋아요한 상품 목록 지원
    // --------------------------

    /** 내가 좋아요한 상품 총 개수 */
    public long getUserLikesTotal(String userEmail) {
        Long size = redis.opsForSet().size(userProductsKey(userEmail));
        return size == null ? 0L : size;
    }

    /**
     * 내가 좋아요한 상품 id 목록 페이징 (간단 버전: SMEMBERS 후 정렬/슬라이싱)
     * - 규모가 커지면 ZSET(시간/ID 스코어)로 교체 권장
     */
    public List<Long> getUserLikedProductIds(String userEmail, int offset, int limit) {
        Set<String> raw = redis.opsForSet().members(userProductsKey(userEmail));
        if (raw == null || raw.isEmpty()) return List.of();

        List<Long> ids = raw.stream()
                .map(s -> { try { return Long.parseLong(s); } catch (Exception e) { return null; }})
                .filter(Objects::nonNull)
                .sorted(Comparator.reverseOrder()) // 큰 ID(최근) 우선
                .collect(Collectors.toList());

        int from = Math.min(offset, ids.size());
        int to   = Math.min(offset + limit, ids.size());
        return ids.subList(from, to);
    }


    public record LikeResult(boolean liked, long count) {}
}
