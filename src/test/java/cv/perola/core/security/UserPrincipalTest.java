package cv.perola.core.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPrincipalTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "maria.silva@perola.cv";
    private static final String PASSWORD = "hashedPassword123";

    @Test
    @DisplayName("Deve mapear os papéis do utilizador para authorities com o prefixo ROLE_")
    void shouldCreateAuthoritiesWithRolePrefix() {
        // Arrange
        List<String> roles = List.of("CUSTOMER", "OWNER");

        // Act
        UserPrincipal userPrincipal = new UserPrincipal(USER_ID, EMAIL, PASSWORD, roles, List.of(), true);

        // Assert
        List<String> authorityNames = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        assertThat(authorityNames).containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_OWNER");
    }

    @Test
    @DisplayName("Deve inicializar listas vazias quando roles forem nulos")
    void shouldHandleNullRolesSafely() {
        // Arrange & Act
        UserPrincipal userPrincipal = new UserPrincipal(USER_ID, EMAIL, PASSWORD, null, List.of(), true);

        // Assert
        assertThat(userPrincipal.getRoles()).isEmpty();
        assertThat(userPrincipal.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("Deve inicializar lista vazia quando memberships forem nulas")
    void shouldHandleNullMembershipsSafely() {
        // Arrange & Act
        UserPrincipal userPrincipal = new UserPrincipal(USER_ID, EMAIL, PASSWORD, List.of("CUSTOMER"), null, true);

        // Assert
        assertThat(userPrincipal.getMemberships()).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar o email como username do UserDetails")
    void shouldReturnEmailAsUsername() {
        // Arrange
        UserPrincipal userPrincipal = new UserPrincipal(USER_ID, EMAIL, PASSWORD, List.of("CUSTOMER"), List.of(), true);

        // Act
        String username = userPrincipal.getUsername();

        // Assert
        assertThat(username).isEqualTo(EMAIL);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Deve refletir o estado de active no método isEnabled")
    void shouldReflectActiveStateInIsEnabled(boolean isActive) {
        // Arrange
        UserPrincipal userPrincipal = new UserPrincipal(USER_ID, EMAIL, PASSWORD, List.of("CUSTOMER"), List.of(), isActive);

        // Act
        boolean enabled = userPrincipal.isEnabled();

        // Assert
        assertThat(enabled).isEqualTo(isActive);
    }

    @Test
    @DisplayName("Deve sempre retornar true para isAccountNonExpired")
    void shouldAlwaysReturnTrueForAccountNonExpired() {
        // Arrange
        UserPrincipal userPrincipal = new UserPrincipal(USER_ID, EMAIL, PASSWORD, List.of("CUSTOMER"), List.of(), true);

        // Act
        boolean accountNonExpired = userPrincipal.isAccountNonExpired();

        // Assert
        assertThat(accountNonExpired).isTrue();
    }

    @Test
    @DisplayName("Deve sempre retornar true para isAccountNonLocked")
    void shouldAlwaysReturnTrueForAccountNonLocked() {
        // Arrange
        UserPrincipal userPrincipal = new UserPrincipal(USER_ID, EMAIL, PASSWORD, List.of("CUSTOMER"), List.of(), true);

        // Act
        boolean accountNonLocked = userPrincipal.isAccountNonLocked();

        // Assert
        assertThat(accountNonLocked).isTrue();
    }

    @Test
    @DisplayName("Deve sempre retornar true para isCredentialsNonExpired")
    void shouldAlwaysReturnTrueForCredentialsNonExpired() {
        // Arrange
        UserPrincipal userPrincipal = new UserPrincipal(USER_ID, EMAIL, PASSWORD, List.of("CUSTOMER"), List.of(), true);

        // Act
        boolean credentialsNonExpired = userPrincipal.isCredentialsNonExpired();

        // Assert
        assertThat(credentialsNonExpired).isTrue();
    }

    @Test
    @DisplayName("Deve proteger imutabilidade das listas internas contra modificações externas")
    void shouldProtectImmutabilityOfRolesList() {
        // Arrange
        List<String> mutableRoles = new ArrayList<>();
        mutableRoles.add("CUSTOMER");
        UserPrincipal userPrincipal = new UserPrincipal(USER_ID, EMAIL, PASSWORD, mutableRoles, List.of(), true);

        // Act
        mutableRoles.add("OWNER");

        // Assert
        assertThat(userPrincipal.getRoles()).containsExactly("CUSTOMER");
        assertThatThrownBy(() -> userPrincipal.getRoles().add("EMPLOYEE"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
