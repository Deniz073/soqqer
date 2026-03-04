package nl.quintor.soqqer.employee.gateway.api.dto;

import nl.quintor.soqqer.employee.persistence.entity.Office;

import java.io.Serializable;

/**
 * DTO for {@link nl.quintor.soqqer.employee.persistence.entity.Employee}
 */
public record EmployeeDTO(Long id, String name, Office office) {
}