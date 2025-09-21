package com.ecommerce.project.controller;

import com.ecommerce.project.payload.ProductCardDTO;
import com.ecommerce.project.service.like.ProductLikeQueryService;
import com.ecommerce.project.service.like.RedisLikeService;
import com.ecommerce.project.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MyPageController {

    @Autowired
    private AuthUtil authUtil;

    private final ProductLikeQueryService likeService;

    @GetMapping("/likes/my")
    public Page<ProductCardDTO> myLikes(Pageable pageable) {
        String email = authUtil.loggedInEmail();
        return likeService.myLikedProducts(email, pageable);
    }
}
