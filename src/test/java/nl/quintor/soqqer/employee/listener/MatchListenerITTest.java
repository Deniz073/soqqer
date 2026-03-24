package nl.quintor.soqqer.employee.listener;

import nl.quintor.soqqer.common.events.match.MatchFinishedEvent;
import nl.quintor.soqqer.config.TestcontainersConfiguration;
import nl.quintor.soqqer.employee.EmployeeLookup;
import nl.quintor.soqqer.employee.listeners.MatchListener;
import nl.quintor.soqqer.employee.persistence.entity.Employee;
import nl.quintor.soqqer.employee.persistence.entity.Office;
import nl.quintor.soqqer.employee.persistence.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
@ActiveProfiles("it")
@Import(TestcontainersConfiguration.class)
class MatchListenerITTest {
    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void beforeEach() {
        employeeRepository.deleteAll();
    }

    @Test
    void onMatchFinishedEvent_Calculates_New_elo_For_Players(Scenario scenario) {
        var employee1 = Employee.builder().name("Player 1").office(Office.DENBOSCH).elo(1000).crawlCounter(0).build();
        var employee2 = Employee.builder().name("Player 2").office(Office.DENBOSCH).elo(1000).crawlCounter(0).build();
        employeeRepository.saveAll(List.of(employee1, employee2));

        MatchFinishedEvent event = new MatchFinishedEvent(
                5,
                3,
                List.of(employee1.getId()),
                List.of(employee2.getId())
        );

        scenario.publish(event)
                .andWaitAtMost(Duration.ofSeconds(5))
                .andWaitForStateChange(() ->
                        employeeRepository.findById(employee1.getId())
                                .map(Employee::getElo)
                                .orElseThrow(),
                        elo -> elo != 1000
                )
                .andVerify(playerOneElo -> {
                    var playerTwoElo = employeeRepository.findById(employee2.getId()).orElseThrow().getElo();

                    assertThat(playerOneElo).isGreaterThan(1000);
                    assertThat(playerTwoElo).isLessThan(1000);
                });
    }

    @Test
    void onMatchFinishedEvent_Increments_Crawl_Counter(Scenario scenario) {
        var employee1 = Employee.builder().name("Player 1").office(Office.DENBOSCH).elo(1000).crawlCounter(0).build();
        var employee2 = Employee.builder().name("Player 2").office(Office.DENBOSCH).elo(1000).crawlCounter(0).build();
        employeeRepository.saveAll(List.of(employee1, employee2));

        MatchFinishedEvent event = new MatchFinishedEvent(
                10,
                0,
                List.of(employee1.getId()),
                List.of(employee2.getId())
        );

        scenario.publish(event)
                .andWaitAtMost(Duration.ofSeconds(5))
                .andWaitForStateChange(() ->
                        employeeRepository.findById(employee2.getId())
                                .map(Employee::getCrawlCounter)
                                .orElseThrow(),
                        counter -> counter != 0)
                .andVerify(playerTwoCrawlCount -> {
                    var playerOneCrawlCount = employeeRepository.findById(employee1.getId()).orElseThrow().getCrawlCounter();

                    assertThat(playerOneCrawlCount).isZero();
                    assertThat(playerTwoCrawlCount).isEqualTo(1);
                });
    }

}
