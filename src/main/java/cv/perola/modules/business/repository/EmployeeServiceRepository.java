package cv.perola.modules.business.repository;

import cv.perola.modules.business.domain.EmployeeServiceEntity;
import cv.perola.modules.business.domain.EmployeeServiceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeServiceRepository extends JpaRepository<EmployeeServiceEntity, EmployeeServiceId> {

    interface ServiceEmployeeAssignmentProjection {
        UUID getServiceId();
        UUID getEmployeeId();
    }

    List<EmployeeServiceEntity> findAllByBusinessIdAndEmployeeId(UUID businessId, UUID employeeId);

    List<EmployeeServiceEntity> findAllByEmployeeId(UUID employeeId);

    List<EmployeeServiceEntity> findAllByBusinessIdAndServiceId(UUID businessId, UUID serviceId);

    List<EmployeeServiceEntity> findAllByServiceId(UUID serviceId);

    void deleteAllByBusinessIdAndEmployeeId(UUID businessId, UUID employeeId);

    boolean existsByBusinessIdAndEmployeeIdAndServiceId(UUID businessId, UUID employeeId, UUID serviceId);

    @Query("""
        SELECT es.serviceId AS serviceId, es.employeeId AS employeeId
        FROM EmployeeServiceEntity es
        JOIN EmployeeEntity e ON e.id = es.employeeId
        WHERE es.businessId = :businessId
          AND es.serviceId IN :serviceIds
          AND e.isActive = true
    """)
    List<ServiceEmployeeAssignmentProjection> findActiveAssignmentsByBusinessIdAndServiceIdIn(
            @Param("businessId") UUID businessId,
            @Param("serviceIds") Collection<UUID> serviceIds
    );

    @Query("""
        SELECT es.serviceId AS serviceId, es.employeeId AS employeeId
        FROM EmployeeServiceEntity es
        WHERE es.businessId = :businessId
          AND es.employeeId IN :employeeIds
    """)
    List<ServiceEmployeeAssignmentProjection> findAssignmentsByBusinessIdAndEmployeeIdIn(
            @Param("businessId") UUID businessId,
            @Param("employeeIds") Collection<UUID> employeeIds
    );
}
