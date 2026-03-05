package nl.quintor.soqqer.match.events;

import java.util.List;

public record MatchFinishedEvent(Integer teamOneScore, Integer teamTwoScore, List<Long> teamOnePlayerIds, List<Long> teamTwoPlayerIds) {
}
