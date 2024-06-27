package axl.ferns.server.player;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PlayerField {

    String name();

    Class<?> type();

    boolean isStatic() default false;

    boolean isVolatile() default false;

}
