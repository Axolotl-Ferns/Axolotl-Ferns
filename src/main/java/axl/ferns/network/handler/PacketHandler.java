package axl.ferns.network.handler;

import java.net.DatagramPacket;
import java.util.function.Consumer;

public interface PacketHandler extends Consumer<DatagramPacket> {
}
