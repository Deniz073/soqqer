package nl.quintor.soqqer.employee.gateway.api.dto;

/**
 * Minimal employee payload for frontend select inputs.
 */
public record EmployeeSelectDTO(Long id, String name, String office) {
}
