package axl.ferns.network.secure;

public interface Secure {

    byte[] encode(byte[] value, byte[] key);

    byte[] decode(byte[] value, byte[] key);

}
