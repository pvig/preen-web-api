package com.pvig.preenWebApi.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new UserProfileDto(
                user.getId().toString(),
                user.getName(),
                user.getEmail()
        ));
    }

    @GetMapping
    public ResponseEntity<List<UserProfileDto>> listUsers() {
        List<UserProfileDto> users = userRepository.findAll().stream()
                .map(u -> new UserProfileDto(u.getId().toString(), u.getName(), u.getEmail()))
                .toList();
        return ResponseEntity.ok(users);
    }
}
