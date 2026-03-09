package nl.quintor.soqqer.match.gateway.api;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.quintor.soqqer.match.gateway.api.dto.CreateMatchDTO;
import nl.quintor.soqqer.match.gateway.api.dto.MatchDTO;
import nl.quintor.soqqer.match.gateway.api.dto.UpdateMatchDTO;
import nl.quintor.soqqer.match.persistence.service.MatchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {
    private final MatchService matchService;

    @GetMapping
    public ResponseEntity<Page<MatchDTO>> find(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(matchService.find(pageable));
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<MatchDTO> updateMatch(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMatchDTO request
    ) {
        return ResponseEntity.ok(matchService.update(id, request));
    }
}
