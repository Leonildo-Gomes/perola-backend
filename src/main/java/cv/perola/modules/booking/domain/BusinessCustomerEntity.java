package cv.perola.modules.booking.domain;

import cv.perola.core.shared.BaseEntity;
import cv.perola.modules.booking.domain.enums.CustomerTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Ficha CRM multi-tenant que armazena a classificação (tier) e notas privadas
 * que um salão específico atribui a um cliente.
 */
@Entity
@Table(name = "business_customers")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BusinessCustomerEntity extends BaseEntity {

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tier", nullable = false, columnDefinition = "customer_tier")
    private CustomerTier tier = CustomerTier.NEW;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;
}
