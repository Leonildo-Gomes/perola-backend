package cv.perola.modules.identity.repository;

import cv.perola.modules.identity.domain.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    Optional<CustomerEntity> findByEmailIgnoreCase(String email);

    Optional<CustomerEntity> findByUserId(UUID userId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUserId(UUID userId);

    @org.springframework.data.jpa.repository.Query("""
                SELECT c.id FROM CustomerEntity c
                WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR c.phone LIKE CONCAT('%', :search, '%')
            """)
    List<UUID> findCustomerIdsMatching(@Param("search") String search);
}
