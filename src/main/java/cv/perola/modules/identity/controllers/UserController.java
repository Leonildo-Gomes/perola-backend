package cv.perola.modules.identity.controllers;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.identity.dto.UserProfileResponse;
import cv.perola.modules.identity.usecase.GetCurrentUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AuthenticationFailedException("Autenticação necessária.");
        }
        UserProfileResponse response = getCurrentUserUseCase.execute(principal.getId());
        return ResponseEntity.ok(response);
    }
}
