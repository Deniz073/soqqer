package nl.quintor.soqqer.employee.listeners;

import lombok.RequiredArgsConstructor;
import nl.quintor.soqqer.employee.persistence.service.EmployeeService;
import nl.quintor.soqqer.match.events.MatchFinishedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchListener {
    private final EmployeeService employeeService;

    @ApplicationModuleListener
    void on(MatchFinishedEvent event) {
        employeeService.calculateNewPlayerElos(event);
    }
}
