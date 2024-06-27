package axl.ferns.network.packet;

import axl.ferns.network.utils.BinaryStream;
import lombok.Getter;
import lombok.Setter;

public abstract class DataPacket extends BinaryStream implements Cloneable {

    @Getter
    @Setter
    private Target target = Target.SERVER;

    public enum Target {
        SERVER,
        CLIENT
    }

    @Getter
    @Setter
    private String address;

    @Getter
    @Setter
    private int port;

    public DataPacket() {
    }

    public volatile boolean isEncoded = false;

    public abstract short pid();

    public abstract void decode();

    public abstract void encode();

    public final void tryEncode() {
        if (!this.isEncoded) {
            this.isEncoded = true;
            this.putInt(pid());
            this.encode();
        }
    }

    public DataPacket clone() {
        try {
            DataPacket packet = (DataPacket)super.clone();
            packet.setBuffer(this.getBuffer());
            packet.offset = this.offset;
            packet.count = this.count;
            return packet;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

}
