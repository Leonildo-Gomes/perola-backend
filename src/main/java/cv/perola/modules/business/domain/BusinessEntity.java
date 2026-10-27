package cv.perola.modules.business.domain;

import cv.perola.core.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Entity
@Table(name = "businesses")
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BusinessEntity extends BaseEntity {

    @Column(name = "owner_id", nullable = false, unique = true)
    private UUID ownerId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "island", nullable = false, length = 50)
    private String island;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "location", nullable = false, columnDefinition = "TEXT")
    private String location;

    @Column(name = "contact_phone", nullable = false, length = 50)
    private String contactPhone;

    @Builder.Default
    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "Atlantic/Cape_Verde";

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
