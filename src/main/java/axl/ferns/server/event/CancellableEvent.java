package axl.ferns.server.event;

import lombok.Getter;
import lombok.Setter;

public class CancellableEvent extends Event {

    @Getter
    @Setter
    private volatile boolean cancelled;

}
