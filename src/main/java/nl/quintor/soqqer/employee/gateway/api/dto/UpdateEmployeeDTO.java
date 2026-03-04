package nl.quintor.soqqer.employee.gateway.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import nl.quintor.soqqer.employee.persistence.entity.Office;

/**
 * DTO for {@link nl.quintor.soqqer.employee.persistence.entity.Employee}
 */
public record UpdateEmployeeDTO(
        @Size(message = "Naam moet tussen 1 en 255 karakters zijn.", min = 1, max = 255)
        @NotNull( message = "Naam is verplicht.")
        String name,
        @NotNull( message = "Kantoor is verplicht.")
        Office office
) {
}
