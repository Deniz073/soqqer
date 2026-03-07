package nl.quintor.soqqer.match.persistence.mapper;

import org.mapstruct.*;
import nl.quintor.soqqer.employee.EmployeeMTO;
import nl.quintor.soqqer.match.gateway.api.dto.MatchPlayerDTO;
import nl.quintor.soqqer.match.persistence.entity.Match;
import nl.quintor.soqqer.match.gateway.api.dto.CreateMatchDTO;
import nl.quintor.soqqer.match.gateway.api.dto.MatchDTO;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MatchMapper {
    Match toEntity(CreateMatchDTO createMatchDTO);

    MatchDTO toDto(Match match);

    @AfterMapping
    default void linkPlayers(@MappingTarget Match match) {
        match.getPlayers().forEach(player -> player.setMatch(match));
    }

    default MatchDTO toDtoWithEmployees(Match match, Map<Long, EmployeeMTO> employeeMap) {
        var matchDTO = toDto(match);
        List<MatchPlayerDTO> players = match.getPlayers().stream()
                .map(player -> new MatchPlayerDTO(employeeMap.get(player.getEmployeeId()), player.getTeam()))
                .toList();
        matchDTO.setPlayers(players);
        return matchDTO;
    }

}