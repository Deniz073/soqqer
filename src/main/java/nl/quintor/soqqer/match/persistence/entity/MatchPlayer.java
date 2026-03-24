package nl.quintor.soqqer.match.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import nl.quintor.soqqer.common.BaseEntity;

@Entity
@Getter
@Setter
@Table(
        name = "match_players",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_match_players_match_employee",
                        columnNames = {"match_id", "employee_id"}
                )
        }
)
public class MatchPlayer extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "employee_id")
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "team", nullable = false)
    private MatchTeam team;
}
