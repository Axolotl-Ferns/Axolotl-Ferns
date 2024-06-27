package axl.ferns.network.secure;

public class XORSecure implements Secure {

    @Override
    public byte[] encode(byte[] value, byte[] key) {
        return xorOperation(value, key);
    }

    @Override
    public byte[] decode(byte[] value, byte[] key) {
        return xorOperation(value, key); // Дешифрование в данном случае такое же, как и шифрование
    }

    private static byte[] xorOperation(byte[] value, byte[] key) {
        byte[] result = new byte[value.length];
        for (int i = 0; i < value.length; i++)
            result[i] = (byte) (value[i] ^ key[i % key.length]);

        return result;
    }

}