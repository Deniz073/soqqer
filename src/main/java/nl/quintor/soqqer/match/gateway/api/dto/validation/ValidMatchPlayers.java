package nl.quintor.soqqer.match.gateway.api.dto.validation;

import java.lang.annotation.*;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;


@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidMatchPlayersValidator.class)
@Documented
public @interface ValidMatchPlayers {

    String message() default "Ongeldige spelerssamenstelling.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
