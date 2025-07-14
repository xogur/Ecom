package com.ecommerce.project.service;

import com.ecommerce.project.payload.UserInfoResponse;
import java.util.List;

public interface UserService {
    List<UserInfoResponse> getAllUsers();
}
