package nl.quintor.soqqer.employee.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import nl.quintor.soqqer.common.BaseEntity;

@Entity
@Getter
@Setter
@Builder
@Table(
        name = "employees",
        uniqueConstraints = @UniqueConstraint(name = "uk_employees_name_office", columnNames = {"name", "office"})
)
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends BaseEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "office", nullable = false)
    private Office office;

    @Builder.Default
    @Column(name = "elo", nullable = false)
    private Integer elo = 1000;

    @Builder.Default
    @Column(name = "crawl_counter", nullable = false)
    private Integer crawlCounter = 0;

}
