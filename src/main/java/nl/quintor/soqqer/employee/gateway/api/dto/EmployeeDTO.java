package nl.quintor.soqqer.employee.gateway.api.dto;

/**
 * DTO for {@link nl.quintor.soqqer.employee.persistence.entity.Employee}
 */
public record EmployeeDTO(Long id, String name, String office, Integer elo, Integer crawlCounter) {
}
