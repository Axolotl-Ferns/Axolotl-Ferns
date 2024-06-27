package axl.ferns.server.event.player;

import axl.ferns.server.event.Event;
import axl.ferns.server.player.Player;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class PlayersLoadEvent extends Event {

    @Getter
    private final List<Player> players = new ArrayList<>();

}

