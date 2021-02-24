package kieran.config;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;


@Target({ElementType.FIELD, ElementType.PARAMETER,ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {DateTime.DateTimeValidator.class})
public @interface DateTime {

    String message() default "时间格式错误，正确格式yyyy-MM-dd HH:mm:ss";

    String format() default "yyyy-MM-dd HH:mm:ss";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DateTimeValidator implements ConstraintValidator<DateTime, String> {

        private String format;

        @Override
        public void initialize(DateTime dateTime) {
            format = dateTime.format();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {

            if (value == null) {
                return true;
            }
            if (value.length() != format.length()) {
                return false;
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
            try {
                simpleDateFormat.parse(value);
            } catch (ParseException e) {
                return false;
            }
            return true;
        }
    }
}
