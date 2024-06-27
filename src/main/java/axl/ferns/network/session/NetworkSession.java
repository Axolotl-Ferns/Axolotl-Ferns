package axl.ferns.network.session;

import axl.ferns.network.packet.DataPacket;

import java.util.Queue;

public interface NetworkSession {

    boolean isClosed();

    void sendPacket(DataPacket packet);

    void disconnect(String reason);

    Queue<DataPacket> getInbound();

    Queue<DataPacket> getOutbound();

}
