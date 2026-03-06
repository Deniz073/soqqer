package nl.quintor.soqqer.match.persistence.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchMapper matchMapper;
    @Mock
    private EmployeeLookup employeeLookup;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @InjectMocks
    private MatchService matchService;

    @Test
    void find_Returns_Paginated_Matches() {
        var pageable = PageRequest.of(0, 10, Sort.by("id"));

        var match = createMatch(
                1L,
                10,
                8,
                createPlayer(11L, MatchTeam.TEAM_ONE),
                createPlayer(22L, MatchTeam.TEAM_TWO)
        );

        var mappedDto = new MatchDTO();
        mappedDto.setId(1L);
        mappedDto.setTeamOneScore(10);
        mappedDto.setTeamTwoScore(8);

        var employeeMap = Map.of(
                11L, new EmployeeMTO("Player One", null),
                22L, new EmployeeMTO("Player Two", null)
        );

        when(matchRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of(match)));
        when(employeeLookup.findEmployees(Set.of(11L, 22L))).thenReturn(employeeMap);
        when(matchMapper.toDtoWithEmployees(match, employeeMap)).thenReturn(mappedDto);

        var result = matchService.find(pageable);

        assertThat(result.getContent()).containsExactly(mappedDto);
        verify(matchRepository).findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test
    void find_Returns_Empty_Page_When_No_Matches_Exist() {
        var pageable = PageRequest.of(1, 5);

        when(matchRepository.findAll(PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of()));
        when(employeeLookup.findEmployees(Set.of())).thenReturn(Map.of());

        var result = matchService.find(pageable);

        assertThat(result.getContent()).isEmpty();
        verify(matchMapper, never()).toDtoWithEmployees(any(), any());
    }

    @Test
    void createMatch_Throws_When_Unknown_Match_Players_Are_Provided() {
        var dto = new CreateMatchDTO(
                10,
                8,
                List.of(
                        new CreateMatchPlayerDTO(1L, MatchTeam.TEAM_ONE),
                        new CreateMatchPlayerDTO(2L, MatchTeam.TEAM_TWO)
                )
        );

        when(employeeLookup.findMissingEmployeeIds(Set.of(1L, 2L))).thenReturn(Set.of(2L));

        assertThatThrownBy(() -> matchService.createMatch(dto))
                .isInstanceOf(UnknownMatchPlayersException.class)
                .hasMessage("One or more player employeeIds do not exist: [2]");

        verify(matchRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void createMatch_Saves_Publishes_Event_And_Returns_Mapped_Dto() {
        var dto = new CreateMatchDTO(
                10,
                8,
                List.of(
                        new CreateMatchPlayerDTO(11L, MatchTeam.TEAM_ONE),
                        new CreateMatchPlayerDTO(22L, MatchTeam.TEAM_TWO),
                        new CreateMatchPlayerDTO(11L, MatchTeam.TEAM_ONE)
                )
        );

        var entityToSave = createMatch(
                null,
                10,
                8,
                createPlayer(11L, MatchTeam.TEAM_ONE),
                createPlayer(22L, MatchTeam.TEAM_TWO)
        );

        var savedMatch = createMatch(
                99L,
                10,
                8,
                createPlayer(11L, MatchTeam.TEAM_ONE),
                createPlayer(22L, MatchTeam.TEAM_TWO)
        );

        var employees = Map.of(
                11L, new EmployeeMTO("Player One", null),
                22L, new EmployeeMTO("Player Two", null)
        );

        var mappedDto = new MatchDTO();
        mappedDto.setId(99L);
        mappedDto.setTeamOneScore(10);
        mappedDto.setTeamTwoScore(8);

        when(employeeLookup.findMissingEmployeeIds(Set.of(11L, 22L))).thenReturn(Set.of());
        when(matchMapper.toEntity(dto)).thenReturn(entityToSave);
        when(matchRepository.save(entityToSave)).thenReturn(savedMatch);
        when(employeeLookup.findEmployees(Set.of(11L, 22L))).thenReturn(employees);
        when(matchMapper.toDtoWithEmployees(savedMatch, employees)).thenReturn(mappedDto);

        var result = matchService.createMatch(dto);

        assertThat(result).isEqualTo(mappedDto);

        var eventCaptor = ArgumentCaptor.forClass(MatchFinishedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        var event = eventCaptor.getValue();
        assertThat(event.teamOneScore()).isEqualTo(10);
        assertThat(event.teamTwoScore()).isEqualTo(8);
        assertThat(event.teamOnePlayerIds()).containsExactly(11L);
        assertThat(event.teamTwoPlayerIds()).containsExactly(22L);
    }

    private Match createMatch(Long id, Integer teamOneScore, Integer teamTwoScore, MatchPlayer... players) {
        var match = new Match();
        match.setId(id);
        match.setTeamOneScore(teamOneScore);
        match.setTeamTwoScore(teamTwoScore);
        match.setPlayers(new LinkedHashSet<>(List.of(players)));
        return match;
    }

    private MatchPlayer createPlayer(Long employeeId, MatchTeam team) {
        var player = new MatchPlayer();
        player.setEmployeeId(employeeId);
        player.setTeam(team);
        return player;
    }
}
