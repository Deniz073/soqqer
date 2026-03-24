package nl.quintor.soqqer.match.persistence.repository;

import nl.quintor.soqqer.match.persistence.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {
}
