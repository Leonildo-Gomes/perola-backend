package cv.perola.modules.identity.controllers;

import cv.perola.modules.identity.dto.AuthResponse;
import cv.perola.modules.identity.dto.GoogleLoginRequest;
import cv.perola.modules.identity.dto.LoginRequest;
import cv.perola.modules.identity.dto.RefreshTokenRequest;
import cv.perola.modules.identity.dto.RegisterRequest;
import cv.perola.modules.identity.usecase.AuthenticateUserUseCase;
import cv.perola.modules.identity.usecase.GoogleOAuth2UseCase;
import cv.perola.modules.identity.usecase.LogoutUseCase;
import cv.perola.modules.identity.usecase.RefreshTokenUseCase;
import cv.perola.modules.identity.usecase.RegisterUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final GoogleOAuth2UseCase googleOAuth2UseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    @Operation(summary = "Register a new user", description = "Register a new user with  the provided data")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = registerUserUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authenticateUserUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = googleOAuth2UseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = refreshTokenUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        logoutUseCase.execute(request);
        return ResponseEntity.noContent().build();
    }
}
