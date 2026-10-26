package cv.perola.modules.identity;

import cv.perola.modules.identity.domain.RefreshTokenEntity;
import cv.perola.modules.identity.domain.UserEntity;
import cv.perola.modules.identity.domain.enums.UserRole;
import cv.perola.modules.identity.domain.UserRoleEntity;
import cv.perola.modules.identity.repository.RefreshTokenRepository;
import cv.perola.modules.identity.repository.UserRepository;
import cv.perola.modules.identity.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class IdentityPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("Deve persistir e recuperar utilizador com sucesso (CRUD)")
    void shouldCreateAndRetrieveUser() {
        UserEntity user = UserEntity.builder()
                .name("Maria Silva")
                .email("maria.silva@perola.cv")
                .phone("+2389912345")
                .passwordHash("$2a$10$hashedpassword")
                .avatarUrl("https://storage.perola.cv/avatars/maria.jpg")
                .isActive(true)
                .build();

        UserEntity saved = userRepository.saveAndFlush(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Maria Silva");
        assertThat(saved.getEmail()).isEqualTo("maria.silva@perola.cv");
        assertThat(saved.isActive()).isTrue();

        Optional<UserEntity> retrieved = userRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getEmail()).isEqualTo("maria.silva@perola.cv");

        // Update
        saved.setName("Maria Silva Santos");
        UserEntity updated = userRepository.saveAndFlush(saved);
        assertThat(updated.getName()).isEqualTo("Maria Silva Santos");

        // Delete
        userRepository.delete(updated);
        userRepository.flush();
        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Deve encontrar utilizador por email e por google_id")
    void shouldFindUserByEmailAndGoogleId() {
        String googleId = "google-sub-" + UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .name("João Mendes")
                .email("joao.mendes@perola.cv")
                .phone("+2389954321")
                .googleId(googleId)
                .isActive(true)
                .build();

        userRepository.saveAndFlush(user);

        assertThat(userRepository.findByEmail("joao.mendes@perola.cv")).isPresent();
        assertThat(userRepository.findByEmail("inexistente@perola.cv")).isEmpty();

        assertThat(userRepository.findByGoogleId(googleId)).isPresent();
        assertThat(userRepository.findByGoogleId("outro-google-id")).isEmpty();

        assertThat(userRepository.existsByEmail("joao.mendes@perola.cv")).isTrue();
        assertThat(userRepository.existsByEmail("outro@perola.cv")).isFalse();

        assertThat(userRepository.existsByPhone("+2389954321")).isTrue();
        assertThat(userRepository.existsByPhone("+2380000000")).isFalse();
    }

    @Test
    @DisplayName("Deve falhar ao persistir utilizador com email duplicado (uk_users_email)")
    void shouldFailWhenInsertingDuplicateEmail() {
        UserEntity user1 = UserEntity.builder()
                .name("Utilizador 1")
                .email("duplicado@perola.cv")
                .phone("+2389911111")
                .build();
        userRepository.saveAndFlush(user1);

        UserEntity user2 = UserEntity.builder()
                .name("Utilizador 2")
                .email("duplicado@perola.cv")
                .phone("+2389922222")
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_users_email");
    }

    @Test
    @DisplayName("Deve persistir e listar papéis de utilizador (UserRoleEntity)")
    void shouldManageUserRoles() {
        UserEntity user = userRepository.saveAndFlush(UserEntity.builder()
                .name("Carlos Owner")
                .email("carlos.owner@perola.cv")
                .phone("+2389933333")
                .build());

        UserRoleEntity roleOwner = UserRoleEntity.builder()
                .userId(user.getId())
                .role(UserRole.OWNER)
                .build();
        UserRoleEntity roleCustomer = UserRoleEntity.builder()
                .userId(user.getId())
                .role(UserRole.CUSTOMER)
                .build();

        userRoleRepository.saveAndFlush(roleOwner);
        userRoleRepository.saveAndFlush(roleCustomer);

        List<UserRoleEntity> roles = userRoleRepository.findAllByUserId(user.getId());
        assertThat(roles).hasSize(2);
        assertThat(roles).extracting(UserRoleEntity::getRole)
                .containsExactlyInAnyOrder(UserRole.OWNER, UserRole.CUSTOMER);

        assertThat(userRoleRepository.existsByUserIdAndRole(user.getId(), UserRole.OWNER)).isTrue();
        assertThat(userRoleRepository.existsByUserIdAndRole(user.getId(), UserRole.EMPLOYEE)).isFalse();

        Optional<UserRoleEntity> foundRole = userRoleRepository.findByUserIdAndRole(user.getId(), UserRole.OWNER);
        assertThat(foundRole).isPresent();
        assertThat(foundRole.get().getCreatedAt()).isNotNull();

        // Tentar duplicar papel (uk_user_roles_user_role)
        UserRoleEntity duplicateRole = UserRoleEntity.builder()
                .userId(user.getId())
                .role(UserRole.OWNER)
                .build();

        assertThatThrownBy(() -> userRoleRepository.saveAndFlush(duplicateRole))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_user_roles_user_role");
    }

    @Test
    @DisplayName("Deve gerir ciclo de vida de refresh token com pesquisa de tokens ativos")
    void shouldManageRefreshTokenLifecycle() {
        UserEntity user = userRepository.saveAndFlush(UserEntity.builder()
                .name("Ana Cliente")
                .email("ana.cliente@perola.cv")
                .phone("+2389944444")
                .build());

        String tokenHash = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();

        RefreshTokenEntity savedToken = refreshTokenRepository.saveAndFlush(token);

        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getCreatedAt()).isNotNull();
        assertThat(savedToken.isValid()).isTrue();
        assertThat(savedToken.isRevoked()).isFalse();
        assertThat(savedToken.isExpired()).isFalse();

        // Pesquisa de token ativo
        Optional<RefreshTokenEntity> activeToken = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash);
        assertThat(activeToken).isPresent();
        assertThat(activeToken.get().getUserId()).isEqualTo(user.getId());

        // Revogar token
        savedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.saveAndFlush(savedToken);

        assertThat(savedToken.isRevoked()).isTrue();
        assertThat(savedToken.isValid()).isFalse();

        // Token revogado não deve ser retornado por findByTokenHashAndRevokedAtIsNull
        Optional<RefreshTokenEntity> revokedSearch = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash);
        assertThat(revokedSearch).isEmpty();

        // Mas ainda é acessível por findByTokenHash
        Optional<RefreshTokenEntity> anySearch = refreshTokenRepository.findByTokenHash(tokenHash);
        assertThat(anySearch).isPresent();
        assertThat(anySearch.get().getRevokedAt()).isNotNull();
    }
}
