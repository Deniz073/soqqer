package nl.quintor.soqqer.match.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import nl.quintor.soqqer.common.BaseEntity;
import org.hibernate.annotations.BatchSize;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "matches")
public class Match extends BaseEntity {
    @Column(name = "team_one_score", nullable = false)
    private Integer teamOneScore;

    @Column(name = "team_two_score", nullable = false)
    private Integer teamTwoScore;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    private Set<MatchPlayer> players = new LinkedHashSet<>();
}
