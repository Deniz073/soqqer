package nl.quintor.soqqer.employee;

import nl.quintor.soqqer.employee.persistence.entity.Office;


/**
 * DTO for {@link nl.quintor.soqqer.employee.persistence.entity.Employee}
 */
public record EmployeeMTO(String name, Office office, Integer elo, Integer crawlCounter) {
}