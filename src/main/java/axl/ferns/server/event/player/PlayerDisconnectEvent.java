package axl.ferns.server.event.player;

import axl.ferns.server.event.Event;
import axl.ferns.server.player.Player;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class PlayerDisconnectEvent extends Event {

    @Getter
    private final Player player;

    @Getter
    @Setter
    private String reason;

}
