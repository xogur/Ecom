package com.ecommerce.project.security.oauth2;

import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        // 구글 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = (String) oAuth2User.getAttributes().get("email");         // ex) xxx@gmail.com
        String name  = (String) oAuth2User.getAttributes().get("name");          // ex) 홍길동
        String sub   = (String) oAuth2User.getAttributes().get("sub");           // 구글 고유 ID

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google account does not have a verified email.");
        }

        // 이미 존재하면 그대로 사용
        Optional<User> existing = userRepository.findByEmail(email);
        User user = existing.orElseGet(() -> {
            // 신규 가입
            User u = new User();
            u.setEmail(email);

            // username: 이메일 로컬파트 기반, 중복 시 suffix
            String baseUsername = email.split("@")[0];
            String username = baseUsername;
            int suffix = 1;
            while (userRepository.existsByUserName(username)) {
                username = baseUsername + "_" + suffix++;
            }
            u.setUserName(username);

            // 랜덤 패스워드(사용 안 해도 엔터티 제약 회피 용도)
            u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

            // 기본 권한 ROLE_USER
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            u.setRoles(Set.of(userRole));

            return userRepository.save(u);
        });

        // OAuth2User 그대로 전달(필요 시 커스텀 OAuth2User 만들어 반환해도 됨)
        return oAuth2User;
    }
}
