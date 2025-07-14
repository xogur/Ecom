package com.ecommerce.project.service;

import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.payload.UserInfoResponse;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<UserInfoResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserInfoResponse(
                        user.getUserId(),
                        user.getUserName(),
                        user.getEmail(),
                        user.getRoles().stream()
                                .map(role -> role.getRoleName().name())
                                .collect(Collectors.toList()),
                        user.getAddresses().stream()
                                .map(address -> new AddressDTO(
                                        address.getAddressId(),
                                        address.getStreet(),
                                        address.getBuildingName(),
                                        address.getCity(),
                                        address.getState(),
                                        address.getCountry(),
                                        address.getPincode()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}
