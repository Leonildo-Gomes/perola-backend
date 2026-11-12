package cv.perola.modules.identity.controllers;

import cv.perola.core.exception.AuthenticationFailedException;
import cv.perola.core.security.UserPrincipal;
import cv.perola.modules.identity.dto.UpdateUserProfileRequest;
import cv.perola.modules.identity.dto.UserProfileResponse;
import cv.perola.modules.identity.usecase.user.GetCurrentUserUseCase;
import cv.perola.modules.identity.usecase.user.UpdateCurrentUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{version}/users")
@RequiredArgsConstructor
@Tag(name = "Auth & Identity", description = "Autenticação, gestão de sessão e dados de perfil do utilizador")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UpdateCurrentUserUseCase updateCurrentUserUseCase;

    @GetMapping(value = "/me", version = "v1")
    @Operation(summary = "Obter perfil do utilizador autenticado")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AuthenticationFailedException("Autenticação necessária.");
        }
        UserProfileResponse response = getCurrentUserUseCase.execute(principal.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(value = "/me", version = "v1")
    @Operation(summary = "Atualizar perfil e foto (avatarUrl) do utilizador autenticado")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        if (principal == null) {
            throw new AuthenticationFailedException("Autenticação necessária.");
        }
        UserProfileResponse response = updateCurrentUserUseCase.execute(principal.getId(), request);
        return ResponseEntity.ok(response);
    }
}
