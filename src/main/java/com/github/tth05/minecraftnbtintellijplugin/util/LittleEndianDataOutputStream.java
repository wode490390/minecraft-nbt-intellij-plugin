package com.github.tth05.minecraftnbtintellijplugin.util;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LittleEndianDataOutputStream extends FilterOutputStream implements DataOutput {

    protected final DataOutputStream stream;

    public LittleEndianDataOutputStream(OutputStream stream) {
        this(new DataOutputStream(stream));
    }

    public LittleEndianDataOutputStream(DataOutputStream stream) {
        super(stream);
        this.stream = stream;
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        this.stream.write(bytes);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        this.stream.write(bytes, offset, length);
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        this.stream.writeBoolean(value);
    }

    @Override
    public void writeByte(int value) throws IOException {
        this.stream.writeByte(value);
    }

    @Override
    public void writeShort(int value) throws IOException {
        this.stream.writeShort(Short.reverseBytes((short) value));
    }

    @Override
    public void writeChar(int value) throws IOException {
        this.stream.writeChar(Character.reverseBytes((char) value));
    }

    @Override
    public void writeInt(int value) throws IOException {
        this.stream.writeInt(Integer.reverseBytes(value));
    }

    @Override
    public void writeLong(long value) throws IOException {
        this.stream.writeLong(Long.reverseBytes(value));
    }

    @Override
    public void writeFloat(float value) throws IOException {
        this.stream.writeInt(Integer.reverseBytes(Float.floatToIntBits(value)));
    }

    @Override
    public void writeDouble(double value) throws IOException {
        this.stream.writeLong(Long.reverseBytes(Double.doubleToLongBits(value)));
    }

    @Override
    public void writeBytes(String string) throws IOException {
        this.stream.writeBytes(string);
    }

    @Override
    public void writeChars(String string) throws IOException {
        this.stream.writeChars(string);
    }

    @Override
    public void writeUTF(String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        this.writeShort(bytes.length);
        this.write(bytes);
    }
}
