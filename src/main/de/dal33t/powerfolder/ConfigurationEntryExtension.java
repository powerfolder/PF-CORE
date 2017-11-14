package de.dal33t.powerfolder;

import java.lang.annotation.*;

/**
 *
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 * @since 11.5 SP 5
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationEntryExtension {
    String name();

    String oldName() default "";

    Class oldType() default Object.class;
}
