package nl.quintor.soqqer.match.persistence.gateway.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for {@link nl.quintor.soqqer.match.persistence.entity.Match}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatchDTO {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer teamOneScore;
    private Integer teamTwoScore;
    private Set<MatchPlayerDTO> players;
}
