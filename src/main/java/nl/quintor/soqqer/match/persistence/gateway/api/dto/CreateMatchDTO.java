package nl.quintor.soqqer.match.persistence.gateway.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import nl.quintor.soqqer.match.persistence.gateway.api.dto.validation.ValidMatchPlayers;

import java.util.List;

/**
 * DTO for {@link nl.quintor.soqqer.match.persistence.entity.Match}
 */
public record CreateMatchDTO(
        @NotNull(message = "Team 1 score is verplicht.")
        @Min(message = "Team 1 score mag niet lager dan 0 zijn.", value = 0)
        Integer teamOneScore,
        @NotNull(message = "Team 2 score is verplicht")
        @Min(message = "Team 2 score mag niet lager dan 0 zijn.", value = 0)
        Integer teamTwoScore,
        @NotNull(message = "Spelers zijn verplicht")
        @ValidMatchPlayers
        @Valid
        List<CreateMatchPlayerDTO> players
) {
}
