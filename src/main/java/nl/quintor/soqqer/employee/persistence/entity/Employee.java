package nl.quintor.soqqer.employee.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import nl.quintor.soqqer.common.BaseEntity;

@Entity
@Getter
@Setter
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_name_office", columnNames = {"name", "office"})
)
public class Employee extends BaseEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "office", nullable = false)
    private Office office;

}
