package nl.quintor.soqqer.common.events.match;

import java.util.List;

public record MatchFinishedEvent(Integer teamOneScore, Integer teamTwoScore, List<Long> teamOnePlayerIds, List<Long> teamTwoPlayerIds) {
}
