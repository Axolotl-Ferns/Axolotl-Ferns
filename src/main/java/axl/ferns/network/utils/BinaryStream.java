package axl.ferns.network.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BinaryStream {
    public int offset;
    private byte[] buffer;
    protected int count;

    public BinaryStream() {
        this.buffer = new byte[32];
        this.offset = 0;
        this.count = 0;
    }

    public BinaryStream(byte[] buffer) {
        this(buffer, 0);
    }

    public BinaryStream(byte[] buffer, int offset) {
        this.buffer = buffer;
        this.offset = offset;
        this.count = buffer.length;
    }

    public void setLength(int length) {
        if (length <= buffer.length)
            return;

        byte[] buffer = this.buffer;
        this.buffer = new byte[length];
        System.arraycopy(buffer, 0, this.buffer, 0, buffer.length);
    }

    public BinaryStream reset() {
        this.offset = 0;
        this.count = 0;
        return this;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
        this.count = buffer == null ? -1 : buffer.length;
    }

    public void setBuffer(byte[] buffer, int offset) {
        this.setBuffer(buffer);
        this.setOffset(offset);
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public byte[] getBuffer() {
        return Arrays.copyOf(this.buffer, this.count);
    }

    public int getCount() {
        return this.count;
    }

    public byte[] get() {
        return this.get(this.count - this.offset);
    }

    public byte[] get(int len) {
        len = Math.min(len, this.getCount() - this.offset);
        this.offset += len;
        return Arrays.copyOfRange(this.buffer, this.offset - len, this.offset);
    }

    public void put(byte[] bytes) {
        if (bytes == null)
            return;

        if (bytes.length + this.count > this.buffer.length)
            setLength(bytes.length + this.count);

        System.arraycopy(bytes, 0, this.buffer, this.count, bytes.length);
        this.count += bytes.length;
    }

    public long getLong() {
        return Binary.readLong(this.get(8));
    }

    public void putLong(long value) {
        this.put(Binary.writeLong(value));
    }

    public int getInt() {
        return Binary.readInt(this.get(4));
    }

    public void putInt(int value) {
        this.put(Binary.writeInt(value));
    }

    public int getTriad() {
        return Binary.readTriad(this.get(3));
    }

    public void putTriad(int value) {
        this.put(Binary.writeTriad(value));
    }

    public int getShort() {
        return Binary.readShort(this.get(2));
    }

    public void putShort(int value) {
        this.put(Binary.writeShort(value));
    }

    public void putFloat(float value) {
        this.put(Binary.writeFloat(value));
    }

    public boolean getBoolean() {
        return this.getByte() == 1;
    }

    public void putBoolean(boolean value) {
        this.putByte((byte)(value ? 1 : 0));
    }

    public byte getByte() {
        return this.buffer[this.offset++];
    }

    public void putByte(byte value) {
        this.put(new byte[]{value});
    }

    public byte[] getByteArray() {
        return this.get(this.getInt());
    }

    public void putByteArray(byte[] value) {
        this.putInt(value.length);
        this.put(value);
    }

    public String getString() {
        return new String(this.getByteArray(), StandardCharsets.UTF_8);
    }

    public void putString(String value) {
        this.putByteArray(value.getBytes(StandardCharsets.UTF_8));
    }

    public boolean eof() {
        return this.offset < 0 || this.offset >= this.buffer.length;
    }

}
