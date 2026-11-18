package cv.perola.modules.identity.repository;

import cv.perola.modules.identity.domain.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    Optional<CustomerEntity> findByEmailIgnoreCase(String email);

    Optional<CustomerEntity> findByUserId(UUID userId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUserId(UUID userId);
}
