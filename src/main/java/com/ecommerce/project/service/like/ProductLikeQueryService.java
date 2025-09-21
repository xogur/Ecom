package com.ecommerce.project.service.like;

import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.QProduct;
import com.ecommerce.project.payload.ProductCardDTO;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductLikeQueryService {

    private final JPAQueryFactory queryFactory;
    private final RedisLikeService likeService;

    public Page<ProductCardDTO> myLikedProducts(String userEmail, Pageable pageable) {
        // 1) Redis에서 내가 찜한 productId 목록 페이징 (간단 오프셋)
        int offset = (int) pageable.getOffset ();
        int limit = pageable.getPageSize ();
        List<Long> pageIds = likeService.getUserLikedProductIds (userEmail, offset, limit);
        long total = likeService.getUserLikesTotal (userEmail);

        if (pageIds.isEmpty ()) {
            return new PageImpl<> (List.of (), pageable, total);
        }

        QProduct p = QProduct.product;

        // 2) DB에서 상세 조회 (정렬은 id desc 기준)
        List<Product> entities = queryFactory
                .selectFrom (p)
                .where (p.productId.in (pageIds))
                .orderBy (p.productId.desc ())
                .fetch ();

        // 3) Redis로 좋아요 수 배치 조회
        Map<Long, Long> counts = likeService.getCounts (pageIds);

        // 4) DTO 매핑 (id desc 기준 정렬)
        //    DB fetch 결과는 in-order가 보장되지 않을 수 있으니 정렬 한 번 더
        entities.sort (Comparator.comparing (Product::getProductId).reversed ());

        List<ProductCardDTO> content = new ArrayList<> (entities.size ());
        for (Product e : entities) {
            content.add (ProductCardDTO.builder ()
                    .productId (e.getProductId ())
                    .productName (e.getProductName ())
                    .image (e.getImage ())
                    .price (e.getPrice ())
                    .specialPrice (e.getSpecialPrice ())
                    .likeCount (counts.getOrDefault (e.getProductId (), 0L))
                    .build ());
        }

        return new PageImpl<> (content, pageable, total);
    }
}