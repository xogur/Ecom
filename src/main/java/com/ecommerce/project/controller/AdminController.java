package com.ecommerce.project.controller;

import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.UserInfoResponse;
import com.ecommerce.project.service.OrderService;
import com.ecommerce.project.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    private final OrderService orderService;

    @GetMapping("/users")
    public ResponseEntity<List<UserInfoResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }



    @GetMapping("/orders")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}
