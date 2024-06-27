package axl.ferns.server.service;

import axl.ferns.server.Priority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {

    String name();

    String version() default "1.0 Dev";

    Priority priority() default Priority.LOW;

}
