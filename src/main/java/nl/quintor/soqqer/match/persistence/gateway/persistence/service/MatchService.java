package nl.quintor.soqqer.match.persistence.gateway.persistence.service;

import lombok.RequiredArgsConstructor;
import nl.quintor.soqqer.employee.EmployeeLookup;
import nl.quintor.soqqer.match.persistence.gateway.api.dto.CreateMatchDTO;
import nl.quintor.soqqer.match.persistence.gateway.api.dto.CreateMatchPlayerDTO;
import nl.quintor.soqqer.match.persistence.gateway.api.dto.MatchDTO;
import nl.quintor.soqqer.match.persistence.exception.UnknownMatchPlayersException;
import nl.quintor.soqqer.match.persistence.gateway.api.dto.MatchPlayerDTO;
import nl.quintor.soqqer.match.persistence.gateway.persistence.mapper.MatchMapper;
import nl.quintor.soqqer.match.persistence.gateway.persistence.repository.MatchRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matchRepository;
    private final MatchMapper matchMapper;
    private final EmployeeLookup employeeLookup;

    public MatchDTO createMatch(CreateMatchDTO dto) {
        var employeeIds = dto.players().stream()
                .map(CreateMatchPlayerDTO::employeeId)
                .collect(Collectors.toSet());

        var missingEmployeeIds = employeeLookup.findMissingEmployeeIds(employeeIds);
        if (!missingEmployeeIds.isEmpty()) {
            throw new UnknownMatchPlayersException(missingEmployeeIds);
        }

        var entity = matchMapper.toEntity(dto);
        var matchDTO = matchMapper.toDto(matchRepository.save(entity));
        var employees = employeeLookup.findEmployees(employeeIds);
        Set<MatchPlayerDTO> matchPlayers = dto.players().stream()
                .map(player -> new MatchPlayerDTO(employees.get(player.employeeId()), player.team()))
                .collect(Collectors.toSet());

        matchDTO.setPlayers(matchPlayers);


        return matchDTO;
    }
}
