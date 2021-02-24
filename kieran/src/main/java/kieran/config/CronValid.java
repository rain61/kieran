package kieran.config;


import org.springframework.scheduling.support.CronSequenceGenerator;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {CronValid.CronValidator.class})
@Documented
public @interface CronValid {

    String message() default "cron 类型不合法";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class CronValidator implements ConstraintValidator<CronValid, String> {


        @Override
        public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {

            if (value == null) {
                return false;
            }
            try {
                new CronSequenceGenerator(value);
                return  true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
