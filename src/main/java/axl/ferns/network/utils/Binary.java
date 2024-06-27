package axl.ferns.network.utils;

import java.util.Arrays;

public class Binary {
    public Binary() {
    }

    public static int readTriad(byte[] bytes) {
        return readInt(new byte[]{0, bytes[0], bytes[1], bytes[2]});
    }

    public static byte[] writeTriad(int value) {
        return new byte[]{(byte)(value >>> 16 & 255), (byte)(value >>> 8 & 255), (byte)(value & 255)};
    }

    public static short readShort(byte[] bytes) {
        return (short) (((bytes[0] & 255) << 8) + (bytes[1] & 255));
    }

    public static byte[] writeShort(int value) {
        return new byte[]{(byte)(value >>> 8 & 255), (byte)(value & 255)};
    }

    public static int readInt(byte[] bytes) {
        return ((bytes[0] & 255) << 24) + ((bytes[1] & 255) << 16) + ((bytes[2] & 255) << 8) + (bytes[3] & 255);
    }

    public static byte[] writeInt(int value) {
        return new byte[]{(byte)(value >>> 24 & 255), (byte)(value >>> 16 & 255), (byte)(value >>> 8 & 255), (byte)(value & 255)};
    }

    public static float readFloat(byte[] bytes) {
        return Float.intBitsToFloat(readInt(bytes));
    }

    public static byte[] writeFloat(float value) {
        return writeInt(Float.floatToIntBits(value));
    }

    public static double readDouble(byte[] bytes) {
        return Double.longBitsToDouble(readLong(bytes));
    }

    public static byte[] writeDouble(double value) {
        return writeLong(Double.doubleToLongBits(value));
    }

    public static long readLong(byte[] bytes) {
        return ((long)bytes[0] << 56) + ((long)(bytes[1] & 255) << 48) + ((long)(bytes[2] & 255) << 40) + ((long)(bytes[3] & 255) << 32) + ((long)(bytes[4] & 255) << 24) + (long)((bytes[5] & 255) << 16) + (long)((bytes[6] & 255) << 8) + (long)(bytes[7] & 255);
    }

    public static byte[] writeLong(long value) {
        return new byte[]{(byte)((int)(value >>> 56)), (byte)((int)(value >>> 48)), (byte)((int)(value >>> 40)), (byte)((int)(value >>> 32)), (byte)((int)(value >>> 24)), (byte)((int)(value >>> 16)), (byte)((int)(value >>> 8)), (byte)((int)value)};
    }

    public static byte[] reserveBytes(byte[] bytes) {
        byte[] newBytes = new byte[bytes.length];

        for(int i = 0; i < bytes.length; ++i) {
            newBytes[bytes.length - 1 - i] = bytes[i];
        }

        return newBytes;
    }

    public static byte[] subBytes(byte[] bytes, int start, int length) {
        int len = Math.min(bytes.length, start + length);
        return Arrays.copyOfRange(bytes, start, len);
    }

    public static byte[] subBytes(byte[] bytes, int start) {
        return subBytes(bytes, start, bytes.length - start);
    }

    public static byte[][] splitBytes(byte[] bytes, int chunkSize) {
        byte[][] splits = new byte[(bytes.length + chunkSize - 1) / chunkSize][chunkSize];
        int chunks = 0;

        for(int i = 0; i < bytes.length; i += chunkSize) {
            if (bytes.length - i > chunkSize) {
                splits[chunks] = Arrays.copyOfRange(bytes, i, i + chunkSize);
            } else {
                splits[chunks] = Arrays.copyOfRange(bytes, i, bytes.length);
            }

            ++chunks;
        }

        return splits;
    }

}
