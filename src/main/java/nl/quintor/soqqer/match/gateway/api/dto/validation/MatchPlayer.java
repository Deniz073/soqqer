package nl.quintor.soqqer.match.gateway.api.dto.validation;

import nl.quintor.soqqer.match.persistence.entity.MatchTeam;

public interface MatchPlayer {
    Long employeeId();
    MatchTeam team();
}
