package axl.ferns.server.event;

import axl.ferns.server.Priority;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventHandler {

    Priority priority() default Priority.LOW;

}
