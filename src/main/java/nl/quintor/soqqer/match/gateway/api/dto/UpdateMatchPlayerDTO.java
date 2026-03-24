package nl.quintor.soqqer.match.gateway.api.dto;

import jakarta.validation.constraints.NotNull;
import nl.quintor.soqqer.match.gateway.api.dto.validation.MatchPlayer;
import nl.quintor.soqqer.match.persistence.entity.MatchTeam;

/**
 * DTO for {@link nl.quintor.soqqer.match.persistence.entity.MatchPlayer}
 */
public record UpdateMatchPlayerDTO(
        @NotNull(message = "Een speler moet een id hebben.")
        Long employeeId,
        @NotNull(message = "Een speler moet een team hebben.")
        MatchTeam team
) implements MatchPlayer {
}
