package nl.quintor.soqqer.match.persistence.gateway.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.quintor.soqqer.match.persistence.gateway.api.dto.CreateMatchDTO;
import nl.quintor.soqqer.match.persistence.gateway.api.dto.MatchDTO;
import nl.quintor.soqqer.match.persistence.gateway.persistence.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {
    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<MatchDTO> createMatch(@Valid @RequestBody CreateMatchDTO dto) {
        var match = matchService.createMatch(dto);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(match.getId())
                .toUri();

        return ResponseEntity.created(location).body(match);
    }
}
