package nl.quintor.soqqer.employee;

import nl.quintor.soqqer.employee.persistence.entity.Office;

import java.io.Serializable;

/**
 * DTO for {@link nl.quintor.soqqer.employee.persistence.entity.Employee}
 */
public record EmployeeMTO(String name, Office office) implements Serializable {
}