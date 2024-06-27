package axl.ferns.server.event.server;

import axl.ferns.server.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ServerTickEvent extends Event {

    @Getter
    private final long tick;

}
