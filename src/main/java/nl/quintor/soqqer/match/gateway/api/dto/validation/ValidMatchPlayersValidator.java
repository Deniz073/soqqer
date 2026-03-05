package nl.quintor.soqqer.match.gateway.api.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import nl.quintor.soqqer.match.persistence.entity.MatchTeam;
import nl.quintor.soqqer.match.gateway.api.dto.CreateMatchPlayerDTO;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidMatchPlayersValidator
        implements ConstraintValidator<ValidMatchPlayers, List<CreateMatchPlayerDTO>> {

    @Override
    public boolean isValid(List<CreateMatchPlayerDTO> players, ConstraintValidatorContext context) {

        if (players == null) {
            return true;
        }

        int size = players.size();

        if (size != 2 && size != 4) {
            return false;
        }

        // Check unique employee IDs
        Set<Long> employeeIds = new HashSet<>();
        for (CreateMatchPlayerDTO player : players) {
            if (player == null || player.employeeId() == null) {
                return false;
            }

            if (!employeeIds.add(player.employeeId())) {
                return false;
            }
        }

        Map<MatchTeam, Long> teamCounts =
                players.stream()
                        .collect(Collectors.groupingBy(
                                CreateMatchPlayerDTO::team,
                                Collectors.counting()
                        ));

        if (size == 2) {
            return teamCounts.getOrDefault(MatchTeam.TEAM_ONE, 0L) == 1
                    && teamCounts.getOrDefault(MatchTeam.TEAM_TWO, 0L) == 1;
        }

        // size == 4
        return teamCounts.getOrDefault(MatchTeam.TEAM_ONE, 0L) == 2
                && teamCounts.getOrDefault(MatchTeam.TEAM_TWO, 0L) == 2;
    }
}
