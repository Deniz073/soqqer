package nl.quintor.soqqer.employee;

/**
 * DTO for {@link nl.quintor.soqqer.employee.persistence.entity.Employee}
 */
public record EmployeeMTO(String name, String office, Integer elo, Integer crawlCounter) {
}
