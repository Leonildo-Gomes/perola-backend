package cv.perola.modules.business.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employee_services")
@IdClass(EmployeeServiceId.class)
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmployeeServiceEntity {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
