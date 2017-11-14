package de.dal33t.powerfolder;

import java.lang.annotation.*;

/**
 * Use in context of configuration entries.<br />
 * <br />
 *
 *
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 * @since 11.5 SP 5
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValue {
    int intValue() default 0;

    String stringValue() default "";

    boolean booleanValue() default false;


}
