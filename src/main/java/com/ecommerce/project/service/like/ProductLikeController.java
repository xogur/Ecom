package com.ecommerce.project.service.like;

import com.ecommerce.project.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductLikeController {

    private final RedisLikeService likeService;
    private final AuthUtil authUtil; // SecurityContext에서 이메일 꺼내는 유틸 (기존에 사용하셨던 것과 동일)

    /** 단건: 현재 카운트만 조회 (비로그인도 허용 가능) */
    @GetMapping("/{productId}/like/count")
    public Map<String, Object> getCount(@PathVariable Long productId) {
        long c = likeService.getCount(productId);
        return Map.of("productId", productId, "count", c);
    }

    /** 단건: 현재 사용자 기준 좋아요 여부 (로그인 필요) */
    @GetMapping("/{productId}/like/me")
    public Map<String, Object> isLiked(@PathVariable Long productId) {
        String email = authUtil.loggedInEmail();
        boolean liked = likeService.isLiked(email, productId);
        return Map.of("productId", productId, "liked", liked);
    }

    /**
     * 토글 (로그인 필요)
     */
    @PostMapping("/{productId}/like/toggle")
    public RedisLikeService.LikeResult toggle(@PathVariable Long productId) {
        String email = authUtil.loggedInEmail();
        return likeService.toggle(email, productId);
    }

    /** 다건: ids=1,2,3 형태로 카운트/내 좋아요여부 동시 조회 (목록 페이지 최적화) */
    @GetMapping("/likes")
    public Map<String, Object> batch(@RequestParam("ids") List<Long> ids) {
        String email = null;
        try { email = authUtil.loggedInEmail(); } catch (Exception ignore) {}
        Map<Long, Long> counts = likeService.getCounts(ids);
        Map<Long, Boolean> liked = (email != null) ? likeService.getLikedFlags(email, ids) : Map.of();

        return Map.of(
                "counts", counts,          // { 10: 3, 11: 0, ... }
                "liked",  liked            // { 10: true, 11: false, ... } (비로그인 시 비어있음)
        );
    }
}
