package com.ecommerce.project.controller;

import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.jwt.JwtUtils;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.MessageResponse;
import com.ecommerce.project.security.response.UserInfoResponse;
import com.ecommerce.project.security.services.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;



    // 로그인
    @PostMapping("/signin")
    // @RequestBody 는 JSON 형식의 요청 본문을 LoginRequest 에 담는다
    // 예 { "username": "user1", "password": "1234" }
    // ResponseEntity<?>는 HTTP 응답을 다양한 형태로 반환할 수 있는 유연한 반환 타입
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Spring Security의 인증 결과를 담을 변수
        Authentication authentication;
        try {
            //authenticationManager는 스프링 시큐리티에서 제공하는 인증 담당 매니저
            // 사용자가 입력한 ID/PW를 가지고 로그인 처리를 시도합니다.
            // 내부적으로 UserDetailsService → loadUserByUsername() 호출 → DB에서 사용자 정보 조회
            authentication = authenticationManager
                    // UsernamePasswordAuthenticationToken은  Spring Security에서 제공하는 ID + 비밀번호 인증용 토큰 클래스
                    // 로그인 정보를 담아서 authenticationManager에게 넘겨주는 입장권 같은 객체입니다.
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            // 인증 시도(authenticate(...))에서 실패하면 던져지는 예외입니다.
        } catch (AuthenticationException exception) {
            Map<String, Object> map = new HashMap<>();

            // key는 "message"와 "status", value는 각각 "Bad credentials"와 false
            map.put("message", "Bad credentials");
            map.put("status", false);
            return new ResponseEntity<Object>(map, HttpStatus.NOT_FOUND);
        }

        // authentication 객체에는 로그인한 사용자의 정보가 들어 있습니다.
        // 이 정보를 SecurityContext에 저장하면, 이후 요청에서도 @AuthenticationPrincipal 등으로 사용자 정보를 꺼낼 수 있습니다.
        // 쉽게 말해:
        // 쉽게 말해:"지금 로그인한 사람 이거야!" 하고 스프링에게 알려주는 작업입니다.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 인증 객체에서 실제 사용자 정보를 꺼내는 코드입니다.
        // getPrincipal()은 로그인한 사용자 객체를 반환합니다.
        // (UserDetailsImpl)는 형변환
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 로그인한 사용자 정보를 바탕으로 JWT 토큰을 만들고, 이를 쿠키로 생성합니다.
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        // 현재 로그인한 사용자의 권한 목록을 문자열 리스트로 추출합니다.
        // getAuthorities()는 GrantedAuthority 객체 목록을 반환합니다.
        // .map(item -> item.getAuthority())로 문자열만 추출하고 collect로 리스트로 만듭니다.
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // 클라이언트에게 보낼 사용자 정보를 하나의 응답 객체에 담습니다.
        UserInfoResponse response = new UserInfoResponse(userDetails.getId(),
                userDetails.getUsername(), roles, jwtCookie.toString());

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                        jwtCookie.toString())
                .body(response);
    }

    @PostMapping("/signup")
    // @RequestBody SignupRequest signUpRequest: 요청 본문(JSON)을 SignupRequest 객체로 자동 매핑합니다.
    // @Valid: SignupRequest에 선언된 유효성 검증(@NotBlank 등)을 자동으로 실행합니다.
    // ResponseEntity<?>: HTTP 응답을 상태코드와 본문을 포함하여 유연하게 반환합니다.
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {

        if (userRepository.existsByUserName(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);

                        break;
                    case "seller":
                        Role modRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);

                        break;
                    default:
                        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @GetMapping("/username")
    public String currentUserName(Authentication authentication){
        if (authentication != null)
            return authentication.getName();
        else
            return "";
    }


    @GetMapping("/user")
    public ResponseEntity<?> getUserDetails(Authentication authentication){
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        UserInfoResponse response = new UserInfoResponse(userDetails.getId(),
                userDetails.getUsername(), roles);

        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signoutUser(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) {
        // ① 레거시(/api) + 현재(/) 경로 쿠키 모두 삭제
        ResponseCookie delRoot = jwtUtils.getCleanJwtCookieAtRoot();
        ResponseCookie delApi  = jwtUtils.getCleanJwtCookieAtApi();

        // ② 시큐리티 컨텍스트/세션 정리(STATELESS라도 안전)
        new SecurityContextLogoutHandler().logout(request, response, authentication);

        // ③ 다중 Set-Cookie 헤더
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, delRoot.toString())
                .header(HttpHeaders.SET_COOKIE, delApi.toString())
                .body(new MessageResponse("You've been signed out!"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        // 1) 비로그인 처리
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object principal = authentication.getPrincipal();
        Long id = null;
        String username;
        List<String> roles;

        if (principal instanceof UserDetailsImpl u) {
            id = u.getId();
            username = u.getUsername();
            roles = u.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

        } else if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidc) {
            // OAuth2(OIDC) 로그인 케이스
            username = oidc.getEmail(); // 또는 oidc.getPreferredUsername()
            roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            // 필요 시 DB에서 email로 사용자 찾아 id/roles 매핑
            // id = userRepository.findByEmail(username).map(User::getUserId).orElse(null);

        } else if (principal instanceof org.springframework.security.oauth2.core.user.DefaultOAuth2User oauth2) {
            username = String.valueOf(oauth2.getAttributes().getOrDefault("email", oauth2.getName()));
            roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            // 필요 시 DB 조회로 id 매핑
            // id = userRepository.findByEmail(username).map(User::getUserId).orElse(null);

        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(new UserInfoResponse(id, username, roles));
    }

    @GetMapping("/success")
    public ResponseEntity<Void> oauthSuccess(@AuthenticationPrincipal OAuth2User principal) {
        String email = (String) principal.getAttributes().get("email");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(UserDetailsImpl.build(user));

        // 302 + Set-Cookie + Location
        return ResponseEntity.status(302)
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.LOCATION, frontendUrl + "oauth/success?login=success")
                .build();
    }

}