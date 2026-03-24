package nl.quintor.soqqer.match.persistence.service;

import jakarta.persistence.EntityNotFoundException;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
                11L, new EmployeeMTO(1L, "Player One", null, 1000, 0),
                22L, new EmployeeMTO(2L, "Player Two", null, 1000, 0)
        );

        when(matchRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(match)));
        when(employeeLookup.findEmployees(Set.of(11L, 22L))).thenReturn(employeeMap);
        when(matchMapper.toDtoWithEmployees(match, employeeMap)).thenReturn(mappedDto);

        var result = matchService.find(pageable);

        assertThat(result.getContent()).containsExactly(mappedDto);
        verify(matchRepository).findAll(pageable);
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
    void findById_Returns_Match_When_It_Exists() {
        var match = createMatch(
                5L,
                10,
                8,
                createPlayer(11L, MatchTeam.TEAM_ONE),
                createPlayer(22L, MatchTeam.TEAM_TWO)
        );

        var employees = Map.of(
                11L, new EmployeeMTO(1L, "Player One", null, 1000, 0),
                22L, new EmployeeMTO(2L, "Player Two", null, 1000, 0)
        );

        var mappedDto = new MatchDTO();
        mappedDto.setId(5L);
        mappedDto.setTeamOneScore(10);
        mappedDto.setTeamTwoScore(8);

        when(matchRepository.findById(5L)).thenReturn(Optional.of(match));
        when(employeeLookup.findEmployees(Set.of(11L, 22L))).thenReturn(employees);
        when(matchMapper.toDtoWithEmployees(match, employees)).thenReturn(mappedDto);

        var result = matchService.findById(5L);

        assertThat(result).isEqualTo(mappedDto);
    }

    @Test
    void findById_Throws_When_Match_Does_Not_Exist() {
        when(matchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Match with id 99 was not found.");
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
                11L, new EmployeeMTO(1L, "Player One", null, 1000, 0),
                22L, new EmployeeMTO(2L, "Player Two", null, 1000, 0)
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

    @Test
    void update_Throws_When_Match_Does_Not_Exist() {
        var dto = new UpdateMatchDTO(
                10,
                8,
                List.of(
                        new UpdateMatchPlayerDTO(11L, MatchTeam.TEAM_ONE),
                        new UpdateMatchPlayerDTO(22L, MatchTeam.TEAM_TWO)
                )
        );

        when(matchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.update(99L, dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Match with id 99 was not found.");

        verify(matchRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void update_Throws_When_Unknown_Match_Players_Are_Provided() {
        var dto = new UpdateMatchDTO(
                10,
                8,
                List.of(
                        new UpdateMatchPlayerDTO(1L, MatchTeam.TEAM_ONE),
                        new UpdateMatchPlayerDTO(2L, MatchTeam.TEAM_TWO)
                )
        );

        when(matchRepository.findById(5L)).thenReturn(Optional.of(createMatch(5L, 4, 3)));
        when(employeeLookup.findMissingEmployeeIds(Set.of(1L, 2L))).thenReturn(Set.of(2L));

        assertThatThrownBy(() -> matchService.update(5L, dto))
                .isInstanceOf(UnknownMatchPlayersException.class)
                .hasMessage("One or more player employeeIds do not exist: [2]");

        verify(matchRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void update_Saves_And_Returns_Mapped_Dto() {
        var dto = new UpdateMatchDTO(
                8,
                10,
                List.of(
                        new UpdateMatchPlayerDTO(11L, MatchTeam.TEAM_ONE),
                        new UpdateMatchPlayerDTO(22L, MatchTeam.TEAM_TWO)
                )
        );

        var existingMatch = createMatch(
                99L,
                10,
                8,
                createPlayer(11L, MatchTeam.TEAM_ONE),
                createPlayer(22L, MatchTeam.TEAM_TWO)
        );

        var employees = Map.of(
                11L, new EmployeeMTO(1L, "Player One", null, 1000, 0),
                22L, new EmployeeMTO(2L, "Player Two", null, 1000, 0)
        );

        var mappedDto = new MatchDTO();
        mappedDto.setId(99L);
        mappedDto.setTeamOneScore(8);
        mappedDto.setTeamTwoScore(10);

        when(matchRepository.findById(99L)).thenReturn(Optional.of(existingMatch));
        when(employeeLookup.findMissingEmployeeIds(Set.of(11L, 22L))).thenReturn(Set.of());
        when(matchRepository.save(existingMatch)).thenReturn(existingMatch);
        when(employeeLookup.findEmployees(Set.of(11L, 22L))).thenReturn(employees);
        when(matchMapper.toDtoWithEmployees(existingMatch, employees)).thenReturn(mappedDto);

        var result = matchService.update(99L, dto);

        assertThat(result).isEqualTo(mappedDto);
        assertThat(existingMatch.getTeamOneScore()).isEqualTo(8);
        assertThat(existingMatch.getTeamTwoScore()).isEqualTo(10);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void update_Replaces_Removed_Players_And_Reuses_Existing_Player_By_EmployeeId() {
        var existingPlayer = createPlayer(11L, MatchTeam.TEAM_ONE);
        var removedPlayer = createPlayer(22L, MatchTeam.TEAM_TWO);
        var existingMatch = createMatch(99L, 10, 8, existingPlayer, removedPlayer);

        var dto = new UpdateMatchDTO(
                12,
                10,
                List.of(
                        new UpdateMatchPlayerDTO(11L, MatchTeam.TEAM_TWO),
                        new UpdateMatchPlayerDTO(33L, MatchTeam.TEAM_ONE)
                )
        );

        var employees = Map.of(
                11L, new EmployeeMTO(1L, "Player One", null, 1000, 0),
                33L, new EmployeeMTO(3L, "Player Three", null, 1000, 0)
        );

        var mappedDto = new MatchDTO();
        mappedDto.setId(99L);
        mappedDto.setTeamOneScore(12);
        mappedDto.setTeamTwoScore(10);

        when(matchRepository.findById(99L)).thenReturn(Optional.of(existingMatch));
        when(employeeLookup.findMissingEmployeeIds(Set.of(11L, 33L))).thenReturn(Set.of());
        when(matchRepository.save(existingMatch)).thenReturn(existingMatch);
        when(employeeLookup.findEmployees(Set.of(11L, 33L))).thenReturn(employees);
        when(matchMapper.toDtoWithEmployees(existingMatch, employees)).thenReturn(mappedDto);

        matchService.update(99L, dto);

        assertThat(existingMatch.getPlayers()).hasSize(2);
        assertThat(existingMatch.getPlayers().stream().map(MatchPlayer::getEmployeeId)).containsExactlyInAnyOrder(11L, 33L);
        assertThat(existingPlayer.getTeam()).isEqualTo(MatchTeam.TEAM_TWO);
        assertThat(existingMatch.getPlayers().stream().allMatch(player -> player.getMatch() == existingMatch)).isTrue();
    }

    @Test
    void delete_Removes_Match_When_It_Exists() {
        var match = createMatch(55L, 3, 2, createPlayer(11L, MatchTeam.TEAM_ONE), createPlayer(22L, MatchTeam.TEAM_TWO));
        when(matchRepository.findById(55L)).thenReturn(Optional.of(match));

        matchService.delete(55L);

        verify(matchRepository, times(1)).delete(match);
    }

    @Test
    void delete_Throws_When_Match_Does_Not_Exist() {
        when(matchRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.delete(77L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Match with id 77 was not found.");

        verify(matchRepository, never()).delete(any());
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
