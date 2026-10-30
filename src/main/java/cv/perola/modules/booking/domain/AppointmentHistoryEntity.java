package cv.perola.modules.booking.domain;

import cv.perola.modules.booking.domain.enums.AppointmentAction;
import cv.perola.modules.booking.domain.enums.AppointmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade de auditoria imutável (append-only) que regista cada transição no ciclo de vida do agendamento.
 */
@Entity
@Table(name = "appointment_histories")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AppointmentHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "appointment_id", nullable = false, updatable = false)
    private UUID appointmentId;

    @Column(name = "performed_by_id", nullable = false, updatable = false)
    private UUID performedById;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "action", nullable = false, updatable = false, columnDefinition = "appointment_action")
    private AppointmentAction action;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "from_status", updatable = false, columnDefinition = "appointment_status")
    private AppointmentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "to_status", nullable = false, updatable = false, columnDefinition = "appointment_status")
    private AppointmentStatus toStatus;

    @Column(name = "old_start_time", updatable = false)
    private Instant oldStartTime;

    @Column(name = "old_end_time", updatable = false)
    private Instant oldEndTime;

    @Column(name = "new_start_time", updatable = false)
    private Instant newStartTime;

    @Column(name = "new_end_time", updatable = false)
    private Instant newEndTime;

    @Column(name = "notes", updatable = false, columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

