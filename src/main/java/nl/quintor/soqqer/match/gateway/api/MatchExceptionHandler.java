package nl.quintor.soqqer.match.gateway.api;

import lombok.extern.slf4j.Slf4j;
import nl.quintor.soqqer.match.persistence.exception.UnknownMatchPlayersException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(assignableTypes = MatchController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class MatchExceptionHandler {

    @ExceptionHandler(UnknownMatchPlayersException.class)
    public ProblemDetail handleUnknownMatchPlayersException(UnknownMatchPlayersException ex) {
        log.error("Unknown employee id(s) used for match creation", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );

        problemDetail.setTitle("Unknown player employeeId");
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("missingEmployeeIds", ex.getMissingEmployeeIds());

        return problemDetail;
    }
}
