package axl.ferns.server.event.network;

import axl.ferns.network.packet.DataPacket;
import axl.ferns.server.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class PacketEvent extends Event {

    @Getter
    private final DataPacket dataPacket;

}
