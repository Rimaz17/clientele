package dev.rimaz.clientele.customer;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cross-field rule for {@link CustomerQuery}: {@code minAge} must not be greater
 * than {@code maxAge}. The error is reported on {@code maxAge}, so clients can
 * show it next to that input.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidAgeRange.Validator.class)
public @interface ValidAgeRange {

    String message() default "Maximum age must not be less than minimum age";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidAgeRange, CustomerQuery> {

        @Override
        public boolean isValid(CustomerQuery query, ConstraintValidatorContext context) {
            if (query == null || query.minAge() == null || query.maxAge() == null
                    || query.minAge() <= query.maxAge()) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("maxAge")
                    .addConstraintViolation();
            return false;
        }
    }
}
