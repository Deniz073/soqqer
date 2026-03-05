package nl.quintor.soqqer.match.persistence.service;

import lombok.RequiredArgsConstructor;
import nl.quintor.soqqer.employee.EmployeeLookup;
import nl.quintor.soqqer.employee.EmployeeMTO;
import nl.quintor.soqqer.common.events.match.MatchFinishedEvent;
import nl.quintor.soqqer.match.gateway.api.dto.CreateMatchDTO;
import nl.quintor.soqqer.match.gateway.api.dto.CreateMatchPlayerDTO;
import nl.quintor.soqqer.match.gateway.api.dto.MatchDTO;
import nl.quintor.soqqer.match.persistence.entity.Match;
import nl.quintor.soqqer.match.persistence.entity.MatchPlayer;
import nl.quintor.soqqer.match.persistence.entity.MatchTeam;
import nl.quintor.soqqer.match.persistence.exception.UnknownMatchPlayersException;
import nl.quintor.soqqer.match.persistence.mapper.MatchMapper;
import nl.quintor.soqqer.match.persistence.repository.MatchRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matchRepository;
    private final MatchMapper matchMapper;
    private final EmployeeLookup employeeLookup;
    private final ApplicationEventPublisher applicationEventPublisher;

    public Page<MatchDTO> find(Pageable pageable) {
        var recentMatchesPage = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        var matchPage = matchRepository.findAll(recentMatchesPage);
        var employeeMap = fetchEmployeesForMatches(matchPage.getContent());

        return matchPage.map(match -> matchMapper.toDtoWithEmployees(match, employeeMap));
    }

    public MatchDTO createMatch(CreateMatchDTO dto) {
        var employeeIds = dto.players().stream()
                .map(CreateMatchPlayerDTO::employeeId)
                .collect(Collectors.toSet());

        var missingEmployeeIds = employeeLookup.findMissingEmployeeIds(employeeIds);
        if (!missingEmployeeIds.isEmpty()) {
            throw new UnknownMatchPlayersException(missingEmployeeIds);
        }

        var entity = matchMapper.toEntity(dto);
        var savedMatch = matchRepository.save(entity);

        applicationEventPublisher.publishEvent(new MatchFinishedEvent(
                savedMatch.getTeamOneScore(),
                savedMatch.getTeamTwoScore(),
                savedMatch.getPlayers().stream().filter(p -> p.getTeam().equals(MatchTeam.TEAM_ONE)).map(MatchPlayer::getEmployeeId).toList(),
                savedMatch.getPlayers().stream().filter(p -> p.getTeam().equals(MatchTeam.TEAM_TWO)).map(MatchPlayer::getEmployeeId).toList()
        ));

        return matchMapper.toDtoWithEmployees(savedMatch, fetchEmployeesForMatches(List.of(savedMatch)));
    }

    private Map<Long, EmployeeMTO> fetchEmployeesForMatches(List<Match> matches) {
        Set<Long> employeeIds = matches.stream()
                .flatMap(match -> match.getPlayers().stream())
                .map(MatchPlayer::getEmployeeId)
                .collect(Collectors.toSet());

        return employeeLookup.findEmployees(employeeIds);
    }
}
