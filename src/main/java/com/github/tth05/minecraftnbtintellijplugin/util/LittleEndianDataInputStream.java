package com.github.tth05.minecraftnbtintellijplugin.util;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LittleEndianDataInputStream extends FilterInputStream implements DataInput {

    protected final DataInputStream stream;

    public LittleEndianDataInputStream(InputStream stream) {
        this(new DataInputStream(stream));
    }

    public LittleEndianDataInputStream(DataInputStream stream) {
        super(stream);
        this.stream = stream;
    }

    @Override
    public void readFully(@Nonnull byte[] bytes) throws IOException {
        this.stream.readFully(bytes);
    }

    @Override
    public void readFully(@Nonnull byte[] bytes, int offset, int length) throws IOException {
        this.stream.readFully(bytes, offset, length);
    }

    @Override
    public int skipBytes(int amount) throws IOException {
        return this.stream.skipBytes(amount);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return this.stream.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return this.stream.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return this.stream.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return Short.reverseBytes(this.stream.readShort());
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return Short.toUnsignedInt(Short.reverseBytes(this.stream.readShort()));
    }

    @Override
    public char readChar() throws IOException {
        return Character.reverseBytes(this.stream.readChar());
    }

    @Override
    public int readInt() throws IOException {
        return Integer.reverseBytes(this.stream.readInt());
    }

    @Override
    public long readLong() throws IOException {
        return Long.reverseBytes(this.stream.readLong());
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(Integer.reverseBytes(this.stream.readInt()));
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(Long.reverseBytes(this.stream.readLong()));
    }

    @Override
    @Deprecated
    public String readLine() throws IOException {
        return this.stream.readLine();
    }

    @Nonnull
    @Override
    public String readUTF() throws IOException {
        byte[] bytes = new byte[this.readUnsignedShort()];
        this.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
