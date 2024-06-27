package axl.ferns.network.session;

import axl.ferns.server.Server;
import axl.ferns.network.packet.DataPacket;
import axl.ferns.server.event.network.PacketEvent;
import lombok.Getter;
import lombok.Setter;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.jctools.queues.atomic.SpscLinkedAtomicQueue;

import java.util.Queue;

public class Session implements NetworkSession {

    @Getter
    @Setter
    private byte[] key;

    @Getter
    @Setter
    private String keyword;

    @Getter
    @Setter
    private String token;

    @Getter
    private final Queue<DataPacket> inbound = new SpscLinkedAtomicQueue<>();

    @Getter
    private final Queue<DataPacket> outbound = new MpscUnboundedArrayQueue<>(1024);

    @Getter
    @Setter
    private String address;

    @Getter
    @Setter
    private int port;

    @Getter
    @Setter
    private boolean online;

    @Override
    public boolean isClosed() {
        return !online;
    }

    @Override
    public void sendPacket(DataPacket packet) {
        if (!this.isOnline())
            return;

        packet.setTarget(DataPacket.Target.CLIENT);
        Server.getInstance().callEvent(new PacketEvent(packet));
        if (packet.getTarget() == DataPacket.Target.SERVER)
            return;

        packet.tryEncode();
        packet.setBuffer(Server.getInstance().getSecure().encode(packet.getBuffer(), this.key));
        this.getOutbound().offer(packet);
    }

    @Override
    public void disconnect(String reason) {
        if (!isOnline())
            throw new IllegalStateException("[NETWORK] player \"" + address + ":" + port + "\" has already been disconnected.");

        setOnline(false);
    }

}
