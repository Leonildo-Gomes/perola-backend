package cv.perola.modules.business.repository;

import cv.perola.modules.business.domain.EmployeeServiceEntity;
import cv.perola.modules.business.domain.EmployeeServiceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeServiceRepository extends JpaRepository<EmployeeServiceEntity, EmployeeServiceId> {

    List<EmployeeServiceEntity> findAllByBusinessIdAndEmployeeId(UUID businessId, UUID employeeId);

    List<EmployeeServiceEntity> findAllByEmployeeId(UUID employeeId);

    List<EmployeeServiceEntity> findAllByBusinessIdAndServiceId(UUID businessId, UUID serviceId);

    List<EmployeeServiceEntity> findAllByServiceId(UUID serviceId);

    void deleteAllByBusinessIdAndEmployeeId(UUID businessId, UUID employeeId);

    boolean existsByBusinessIdAndEmployeeIdAndServiceId(UUID businessId, UUID employeeId, UUID serviceId);
}
