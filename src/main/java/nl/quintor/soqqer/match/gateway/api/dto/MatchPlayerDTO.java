package nl.quintor.soqqer.match.gateway.api.dto;

import nl.quintor.soqqer.employee.EmployeeMTO;
import nl.quintor.soqqer.match.persistence.entity.MatchTeam;

/**
 * DTO for {@link nl.quintor.soqqer.match.persistence.entity.MatchPlayer}
 */
public record MatchPlayerDTO(EmployeeMTO employee, MatchTeam team) {
}