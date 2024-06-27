package axl.ferns.network.packet.player;

import axl.ferns.network.packet.DataPacket;
import axl.ferns.server.player.Player;
import lombok.Getter;
import lombok.Setter;

public abstract class PlayerPacket extends DataPacket {

    @Getter
    @Setter
    private Player player;

    @Override
    public abstract void encode();

    @Override
    public abstract void decode();

}
