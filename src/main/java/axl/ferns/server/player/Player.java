package axl.ferns.server.player;

import axl.ferns.network.session.Session;
import lombok.Getter;
import lombok.Setter;

public class Player extends Session {

    @Getter
    @Setter
    private long id;

    @Getter
    @Setter
    private String nickname;

}
