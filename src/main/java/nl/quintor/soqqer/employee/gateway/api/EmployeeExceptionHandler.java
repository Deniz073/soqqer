package nl.quintor.soqqer.employee.gateway.api;

import lombok.extern.slf4j.Slf4j;
import nl.quintor.soqqer.employee.persistence.exception.EmployeeAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(assignableTypes = EmployeeController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class EmployeeExceptionHandler {

    @ExceptionHandler(EmployeeAlreadyExistsException.class)
    public ProblemDetail handleEmployeeAlreadyExistsException(EmployeeAlreadyExistsException ex) {
        log.error("Employee already exists error", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );

        problemDetail.setTitle("Employee already exists");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
}
