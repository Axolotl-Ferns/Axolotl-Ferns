package axl.ferns.server.event;

import axl.ferns.server.Priority;

public interface EventExecutor {

    void execute(Event event);

    Class<? extends Event> getArgumentClass();

    Priority getPriority();

}
