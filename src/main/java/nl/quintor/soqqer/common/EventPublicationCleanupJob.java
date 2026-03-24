package nl.quintor.soqqer.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublicationCleanupJob {
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 2 * * *")
    void deleteCompletedEventPublicationsOlderThanSevenDays() {
        int deletedRows = jdbcTemplate.update("""
                DELETE FROM event_publication
                WHERE completion_date IS NOT NULL
                  AND completion_date <= NOW() - INTERVAL '7 days'
                """);

        if (deletedRows > 0) {
            log.info("Deleted {} row(s) from event_publication older than 7 days.", deletedRows);
        }
    }
}
