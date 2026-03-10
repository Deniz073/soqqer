package nl.quintor.soqqer.match.gateway.api;

import nl.quintor.soqqer.config.BaseITTest;
import nl.quintor.soqqer.employee.persistence.entity.Employee;
import nl.quintor.soqqer.employee.persistence.entity.Office;
import nl.quintor.soqqer.employee.persistence.repository.EmployeeRepository;
import nl.quintor.soqqer.match.gateway.api.dto.CreateMatchDTO;
import nl.quintor.soqqer.match.gateway.api.dto.CreateMatchPlayerDTO;
import nl.quintor.soqqer.match.gateway.api.dto.MatchDTO;
import nl.quintor.soqqer.match.gateway.api.dto.UpdateMatchDTO;
import nl.quintor.soqqer.match.gateway.api.dto.UpdateMatchPlayerDTO;
import nl.quintor.soqqer.match.persistence.entity.MatchTeam;
import nl.quintor.soqqer.match.persistence.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES
)
class MatchControllerITTest extends BaseITTest {

    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    private RestTestClient restTestClient;

    @BeforeEach
    void beforeEach() {
        matchRepository.deleteAll();
        employeeRepository.deleteAll();
        restTestClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + port + "/api/matches").build();
    }

    @Test
    void getAllMatches_Returns_Empty_When_No_Matches_Exist() {
        restTestClient.get().exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(0);
    }

    @Test
    void match_Endpoint_Flow_Create_And_GetAll() {
        var playerOne = createEmployee("Player One", Office.DENBOSCH);
        var playerTwo = createEmployee("Player Two", Office.DENHAAG);

        var created = createMatch(new CreateMatchDTO(
                10,
                8,
                List.of(
                        new CreateMatchPlayerDTO(playerOne.getId(), MatchTeam.TEAM_ONE),
                        new CreateMatchPlayerDTO(playerTwo.getId(), MatchTeam.TEAM_TWO)
                )
        ));

        assertThat(created.getTeamOneScore()).isEqualTo(10);
        assertThat(created.getTeamTwoScore()).isEqualTo(8);
        assertThat(created.getPlayers()).hasSize(2);

        restTestClient.get().exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.content[0].id").isEqualTo(created.getId())
                .jsonPath("$.content[0].teamOneScore").isEqualTo(10)
                .jsonPath("$.content[0].teamTwoScore").isEqualTo(8)
                .jsonPath("$.content[0].players.length()").isEqualTo(2);
    }

    @Test
    void getMatchById_Returns_Ok_When_Match_Exists() {
        var playerOne = createEmployee("Player One", Office.DENBOSCH);
        var playerTwo = createEmployee("Player Two", Office.DENHAAG);

        var created = createMatch(new CreateMatchDTO(
                10,
                8,
                List.of(
                        new CreateMatchPlayerDTO(playerOne.getId(), MatchTeam.TEAM_ONE),
                        new CreateMatchPlayerDTO(playerTwo.getId(), MatchTeam.TEAM_TWO)
                )
        ));

        restTestClient.get()
                .uri("/{id}", created.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(created.getId())
                .jsonPath("$.teamOneScore").isEqualTo(10)
                .jsonPath("$.teamTwoScore").isEqualTo(8)
                .jsonPath("$.players.length()").isEqualTo(2);
    }

    @Test
    void getMatchById_Returns_NotFound_When_Match_Does_Not_Exist() {
        restTestClient.get()
                .uri("/{id}", 99L)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Entity Not Found")
                .jsonPath("$.detail").isEqualTo("Entity not found");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void createMatch_Returns_BadRequest_When_Request_Is_Invalid() {
        restTestClient.post()
                .body(new CreateMatchDTO(
                        -1,
                        3,
                        List.of(new CreateMatchPlayerDTO(1L, MatchTeam.TEAM_ONE))
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Validation Error")
                .jsonPath("$.detail").isEqualTo("Validation failed for one or more fields")
                .jsonPath("$.errors.teamOneScore").isEqualTo("Team 1 score mag niet lager dan 0 zijn.")
                .jsonPath("$.errors.players").isEqualTo("Ongeldige spelerssamenstelling.");
    }

    @Test
    void createMatch_Returns_BadRequest_When_Unknown_Player_Is_Used() {
        var playerOne = createEmployee("Known Player", Office.GRONINGEN);

        restTestClient.post()
                .body(new CreateMatchDTO(
                        5,
                        3,
                        List.of(
                                new CreateMatchPlayerDTO(playerOne.getId(), MatchTeam.TEAM_ONE),
                                new CreateMatchPlayerDTO(99999L, MatchTeam.TEAM_TWO)
                        )
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Unknown player employeeId")
                .jsonPath("$.missingEmployeeIds[0]").isEqualTo(99999);
    }

    @Test
    void updateMatch_Returns_Ok_When_Request_Is_Valid() {
        var playerOne = createEmployee("Player One", Office.DENBOSCH);
        var playerTwo = createEmployee("Player Two", Office.DENHAAG);
        var playerThree = createEmployee("Player Three", Office.GRONINGEN);
        var playerFour = createEmployee("Player Four", Office.DEVENTER);

        var created = createMatch(new CreateMatchDTO(
                1,
                0,
                List.of(
                        new CreateMatchPlayerDTO(playerOne.getId(), MatchTeam.TEAM_ONE),
                        new CreateMatchPlayerDTO(playerTwo.getId(), MatchTeam.TEAM_TWO)
                )
        ));

        var updated = updateMatch(created.getId(), new UpdateMatchDTO(
                7,
                5,
                List.of(
                        new UpdateMatchPlayerDTO(playerThree.getId(), MatchTeam.TEAM_ONE),
                        new UpdateMatchPlayerDTO(playerFour.getId(), MatchTeam.TEAM_TWO)
                )
        ));

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getTeamOneScore()).isEqualTo(7);
        assertThat(updated.getTeamTwoScore()).isEqualTo(5);
        assertThat(updated.getPlayers()).hasSize(2);
        assertThat(updated.getPlayers().stream().map(player -> player.employee().name())).containsExactlyInAnyOrder(
                playerThree.getName(),
                playerFour.getName()
        );
    }

    @Test
    void updateMatch_Returns_Ok_When_Request_Contains_Existing_Player_And_New_Player() {
        var playerOne = createEmployee("Player One", Office.DENBOSCH);
        var playerTwo = createEmployee("Player Two", Office.DENHAAG);
        var playerThree = createEmployee("Player Three", Office.GRONINGEN);

        var created = createMatch(new CreateMatchDTO(
                1,
                0,
                List.of(
                        new CreateMatchPlayerDTO(playerOne.getId(), MatchTeam.TEAM_ONE),
                        new CreateMatchPlayerDTO(playerTwo.getId(), MatchTeam.TEAM_TWO)
                )
        ));

        var updated = updateMatch(created.getId(), new UpdateMatchDTO(
                7,
                5,
                List.of(
                        new UpdateMatchPlayerDTO(playerOne.getId(), MatchTeam.TEAM_TWO),
                        new UpdateMatchPlayerDTO(playerThree.getId(), MatchTeam.TEAM_ONE)
                )
        ));

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getTeamOneScore()).isEqualTo(7);
        assertThat(updated.getTeamTwoScore()).isEqualTo(5);
        assertThat(updated.getPlayers()).hasSize(2);
        assertThat(updated.getPlayers().stream().map(player -> player.employee().name())).containsExactlyInAnyOrder(
                playerOne.getName(),
                playerThree.getName()
        );
    }

    @Test
    void updateMatch_Returns_NotFound_When_Match_Does_Not_Exist() {
        var playerOne = createEmployee("Known Player", Office.GRONINGEN);
        var playerTwo = createEmployee("Known Player 2", Office.DENHAAG);

        restTestClient.put()
                .uri("/{id}", 99L)
                .body(new UpdateMatchDTO(
                        5,
                        3,
                        List.of(
                                new UpdateMatchPlayerDTO(playerOne.getId(), MatchTeam.TEAM_ONE),
                                new UpdateMatchPlayerDTO(playerTwo.getId(), MatchTeam.TEAM_TWO)
                        )
                ))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Entity Not Found")
                .jsonPath("$.detail").isEqualTo("Entity not found");
    }

    @Test
    void updateMatch_Returns_BadRequest_When_Request_Is_Invalid() {
        var playerOne = createEmployee("Known Player", Office.GRONINGEN);
        var playerTwo = createEmployee("Known Player 2", Office.DENHAAG);

        var created = createMatch(new CreateMatchDTO(
                1,
                0,
                List.of(
                        new CreateMatchPlayerDTO(playerOne.getId(), MatchTeam.TEAM_ONE),
                        new CreateMatchPlayerDTO(playerTwo.getId(), MatchTeam.TEAM_TWO)
                )
        ));

        restTestClient.put()
                .uri("/{id}", created.getId())
                .body(new UpdateMatchDTO(
                        -1,
                        3,
                        List.of(new UpdateMatchPlayerDTO(playerOne.getId(), MatchTeam.TEAM_ONE))
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Validation Error")
                .jsonPath("$.detail").isEqualTo("Validation failed for one or more fields")
                .jsonPath("$.errors.teamOneScore").isEqualTo("Team 1 score mag niet lager dan 0 zijn.")
                .jsonPath("$.errors.players").isEqualTo("Ongeldige spelerssamenstelling.");
    }

    @Test
    void updateMatch_Returns_BadRequest_When_Unknown_Player_Is_Used() {
        var playerOne = createEmployee("Known Player", Office.GRONINGEN);
        var playerTwo = createEmployee("Known Player 2", Office.DENHAAG);

        var created = createMatch(new CreateMatchDTO(
                1,
                0,
                List.of(
                        new CreateMatchPlayerDTO(playerOne.getId(), MatchTeam.TEAM_ONE),
                        new CreateMatchPlayerDTO(playerTwo.getId(), MatchTeam.TEAM_TWO)
                )
        ));

        restTestClient.put()
                .uri("/{id}", created.getId())
                .body(new UpdateMatchDTO(
                        5,
                        3,
                        List.of(
                                new UpdateMatchPlayerDTO(playerOne.getId(), MatchTeam.TEAM_ONE),
                                new UpdateMatchPlayerDTO(99999L, MatchTeam.TEAM_TWO)
                        )
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Unknown player employeeId")
                .jsonPath("$.missingEmployeeIds[0]").isEqualTo(99999);
    }

    @Test
    void deleteMatch_Returns_NoContent_When_Match_Exists() {
        var playerOne = createEmployee("Player One", Office.DENBOSCH);
        var playerTwo = createEmployee("Player Two", Office.DENHAAG);

        var created = createMatch(new CreateMatchDTO(
                10,
                8,
                List.of(
                        new CreateMatchPlayerDTO(playerOne.getId(), MatchTeam.TEAM_ONE),
                        new CreateMatchPlayerDTO(playerTwo.getId(), MatchTeam.TEAM_TWO)
                )
        ));

        restTestClient.delete()
                .uri("/{id}", created.getId())
                .exchange()
                .expectStatus().isNoContent();

        restTestClient.get()
                .uri("/{id}", created.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteMatch_Returns_NotFound_When_Match_Does_Not_Exist() {
        restTestClient.delete()
                .uri("/{id}", 99L)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Entity Not Found")
                .jsonPath("$.detail").isEqualTo("Entity not found");
    }

    private Employee createEmployee(String name, Office office) {
        return employeeRepository.save(Employee.builder().name(name).office(office).build());
    }

    private MatchDTO createMatch(CreateMatchDTO dto) {
        var result = restTestClient.post()
                .body(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody(MatchDTO.class)
                .returnResult();

        var match = result.getResponseBody();
        assertThat(match).isNotNull();
        assertThat(match.getId()).isNotNull();
        assertThat(result.getResponseHeaders().getLocation()).hasPath("/api/matches/" + match.getId());
        return match;
    }

    private MatchDTO updateMatch(Long matchId, UpdateMatchDTO dto) {
        var result = restTestClient.put()
                .uri("/{id}", matchId)
                .body(dto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MatchDTO.class)
                .returnResult();

        var match = result.getResponseBody();
        assertThat(match).isNotNull();
        assertThat(match.getId()).isEqualTo(matchId);
        return match;
    }
}
