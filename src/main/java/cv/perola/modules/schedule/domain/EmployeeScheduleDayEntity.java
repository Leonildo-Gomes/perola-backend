package cv.perola.modules.schedule.domain;

import cv.perola.core.shared.BaseEntity;
import cv.perola.modules.schedule.domain.enums.DayOfWeek;
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

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "employee_schedule_days")
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmployeeScheduleDayEntity extends BaseEntity {

    @Column(name = "employee_schedule_id", nullable = false)
    private UUID employeeScheduleId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "day_of_week", nullable = false, columnDefinition = "day_of_week")
    private DayOfWeek dayOfWeek;

    @Builder.Default
    @Column(name = "is_working", nullable = false)
    private boolean isWorking = true;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;
}

