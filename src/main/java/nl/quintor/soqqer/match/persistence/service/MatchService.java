package nl.quintor.soqqer.match.persistence.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.quintor.soqqer.employee.EmployeeLookup;
import nl.quintor.soqqer.employee.EmployeeMTO;
import nl.quintor.soqqer.common.events.match.MatchFinishedEvent;
import nl.quintor.soqqer.match.gateway.api.dto.CreateMatchDTO;
import nl.quintor.soqqer.match.gateway.api.dto.CreateMatchPlayerDTO;
import nl.quintor.soqqer.match.gateway.api.dto.MatchDTO;
import nl.quintor.soqqer.match.gateway.api.dto.UpdateMatchDTO;
import nl.quintor.soqqer.match.gateway.api.dto.UpdateMatchPlayerDTO;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
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
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "createdAt");

        var recentMatchesPage = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );

        var matchPage = matchRepository.findAll(recentMatchesPage);
        var employeeMap = fetchEmployeesForMatches(matchPage.getContent());

        return matchPage.map(match -> matchMapper.toDtoWithEmployees(match, employeeMap));
    }

    public MatchDTO findById(Long id) {
        var match = matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match with id " + id + " was not found."));

        return matchMapper.toDtoWithEmployees(match, fetchEmployeesForMatches(List.of(match)));
    }

    @Transactional
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

    @Transactional
    public MatchDTO update(Long id, @Valid UpdateMatchDTO request) {
        var match = matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match with id " + id + " was not found."));

        var employeeIds = request.players().stream()
                .map(UpdateMatchPlayerDTO::employeeId)
                .collect(Collectors.toSet());

        var missingEmployeeIds = employeeLookup.findMissingEmployeeIds(employeeIds);
        if (!missingEmployeeIds.isEmpty()) {
            throw new UnknownMatchPlayersException(missingEmployeeIds);
        }

        match.setTeamOneScore(request.teamOneScore());
        match.setTeamTwoScore(request.teamTwoScore());
        synchronizePlayers(match, request.players());

        var savedMatch = matchRepository.save(match);

        return matchMapper.toDtoWithEmployees(savedMatch, fetchEmployeesForMatches(List.of(savedMatch)));
    }

    @Transactional
    public void delete(Long id) {
        var match = matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match with id " + id + " was not found."));
        matchRepository.delete(match);
    }

    private void synchronizePlayers(Match match, List<UpdateMatchPlayerDTO> requestedPlayers) {
        var existingPlayersByEmployeeId = new HashMap<Long, MatchPlayer>();
        match.getPlayers().forEach(player -> existingPlayersByEmployeeId.put(player.getEmployeeId(), player));

        var requestedEmployeeIds = requestedPlayers.stream()
                .map(UpdateMatchPlayerDTO::employeeId)
                .collect(Collectors.toSet());

        match.getPlayers().removeIf(player -> !requestedEmployeeIds.contains(player.getEmployeeId()));

        for (var requestedPlayer : requestedPlayers) {
            var existingPlayer = existingPlayersByEmployeeId.get(requestedPlayer.employeeId());
            if (existingPlayer != null) {
                existingPlayer.setTeam(requestedPlayer.team());
                existingPlayer.setMatch(match);
                continue;
            }

            var player = new MatchPlayer();
            player.setEmployeeId(requestedPlayer.employeeId());
            player.setTeam(requestedPlayer.team());
            player.setMatch(match);
            match.getPlayers().add(player);
        }
    }

    private Map<Long, EmployeeMTO> fetchEmployeesForMatches(List<Match> matches) {
        Set<Long> employeeIds = matches.stream()
                .flatMap(match -> match.getPlayers().stream())
                .map(MatchPlayer::getEmployeeId)
                .collect(Collectors.toSet());

        return employeeLookup.findEmployees(employeeIds);
    }
}
