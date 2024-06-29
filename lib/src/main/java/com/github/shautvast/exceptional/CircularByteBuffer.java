package com.github.shautvast.exceptional;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Circular buffer for variable sized byte arrays. The indices for read and write
 * are also stored in the bytebuffer, making changes visible to any non-java process that is reading.
 *
 * Written for a scenario with multiple concurrent writers, and a single reader in a non-java process
 * This class itself is Not Threadsafe! It relies on MPSCBufferWriter for multithreaded writes. This queues
 * byte arrays waiting to be stored in the circular buffer. MPSCBufferWriter starts the only
 * thread that is allowed to interact with the CircularByteBuffer.
 * ..
 * *Implementation note:*
 * The first 8 bytes are reserved for the reader and writer index. START constant indicates the actual startindex
 * of the payload data. The index stored is the actual index (ie starting at 8). The reads and write methods
 * for reader/writer index deal with the offset value, so that the index (as method local variable) does not
 * include it (ie starting at 0). This simplifies the calculations that include these indices. Same goes for the
 * capacity.
 */
@SuppressWarnings("StringTemplateMigration")
public class CircularByteBuffer {

    public static final int READ = 0;
    public static final int WRITE = 4;
    public static final int START = 8;
    final ByteBuffer data;

    /**
     * Constructs a CircularByteBuffer with the specified capacity.
     * The buffer is backed by a byte array on the java-heap. Mainly there for test purposes.
     *
     * @param capacity the capacity of the CircularByteBuffer
     */
    public CircularByteBuffer(int capacity) {
        data = ByteBuffer.allocate(capacity + START); // 8 extra for the read and write index
        data.putInt(READ, START);
        data.putInt(WRITE, START);
    }

    /**
     * Constructs a CircularByteBuffer with the specified capacity. The buffer is backed by native memory
     * from the MemorySegment
     */
    public CircularByteBuffer(MemorySegment memory) {
        if (memory.byteSize() > 0xfff7) {
            throw new IllegalArgumentException("Max memory size is 65527");
        }
        this.data = memory.asByteBuffer();
    }

    public boolean put(byte[] bytes) {
        int len = bytes.length;
        int remaining;
        // check capacity for bytes to insert
        int readIndex = getReadIndex();
        int writeIndex = getWriteIndex();
        try {
            if (writeIndex >= readIndex) {
                remaining = capacity() - writeIndex + readIndex;
            } else {
                remaining = readIndex - writeIndex;
            }
            if (remaining < len + 2) {
                return false;
            } else {
                int remainingUntilEnd = capacity() - writeIndex;
                if (remainingUntilEnd < len + 2) {
                    if (remainingUntilEnd > 1) {
                        // we can write the length
                        putShort(writeIndex, (short) len);
                        writeIndex += 2;
                        remainingUntilEnd -= 2;
                        if (remainingUntilEnd > 0) {
                            put(writeIndex, bytes, 0, remainingUntilEnd);
                        }
                        writeIndex = 0;
                        put(writeIndex, bytes, remainingUntilEnd, len);
                        writeIndex += len - remainingUntilEnd;
                    } else {
                        // we can write only one byte of the length
                        put(writeIndex, (byte) (len >> 8));
                        writeIndex = 0;
                        put(writeIndex, (byte) (len & 0xff));
                        writeIndex += 1;
                        put(writeIndex, bytes);
                        writeIndex += len;
                    }
                } else {
                    putShort(writeIndex, (short) len);
                    writeIndex += 2;
                    put(writeIndex, bytes);
                    writeIndex += len;

                    if (writeIndex == capacity()) {
                        writeIndex = 0;
                    }
                }

                return true;
            }
        } finally {
            setWriteIndex(writeIndex);
        }
    }

    private int capacity() {
        return data.capacity() - START;
    }

    /**
     * The reader side is provided, for reference and testability only.
     * In practice, the reader is implemented outside of java
     */
    public byte[] get() {
        int readIndex = getReadIndex();
        try {
            int remainingUntilEnd = capacity() - readIndex;
            int len;
            if (remainingUntilEnd == 1) {
                byte high = get(readIndex);
                readIndex = 0;
                byte low = get(readIndex);
                readIndex += 1;
                len = high << 8 | low;
                remainingUntilEnd = len;
            } else if (remainingUntilEnd == 2) {
                len = getShort(readIndex);
                readIndex = 0;
                remainingUntilEnd = 0;
            } else {
                len = getShort(readIndex);
                readIndex += 2;
                remainingUntilEnd -= 2;
            }
            byte[] result = new byte[len];
            if (len <= remainingUntilEnd) {
                get(readIndex, result);
                readIndex += len;
            } else {
                get(readIndex, result, 0, remainingUntilEnd);
                readIndex = 0;
                get(readIndex, result, remainingUntilEnd, len - remainingUntilEnd);
                readIndex += len - remainingUntilEnd;
            }
            return result;
        } finally {
            setReadIndex(readIndex);
        }
    }

    private void get(int readIndex, byte[] result, int offset, int len) {
        data.get(readIndex + START, result, offset, len);
    }

    private void get(int readIndex, byte[] result) {
        data.get(readIndex + START, result);
    }

    private short getShort(int readIndex) {
        return data.getShort(readIndex + START);
    }

    private byte get(int readIndex) {
        return data.get(readIndex + START);
    }

    int getWriteIndex() {
        return this.data.getInt(WRITE) - START;
    }

    void setWriteIndex(int writeIndex) {
        this.data.putInt(WRITE, writeIndex + START);
    }

    int getReadIndex() {
        return this.data.getInt(READ) - START;
    }

    void setReadIndex(int readIndex) {
        this.data.putInt(READ, readIndex + START);
    }

    void putShort(int index, short value) {
        this.data.putShort(index + START, value);
    }

    void put(int index, byte value) {
        this.data.put(index + START, value);
    }

    void put(int index, byte[] value) {
        this.data.put(index + START, value);
    }

    private void put(int writeIndex, byte[] bytes, int offset, int len) {
        data.put(writeIndex + START, bytes, offset, len - offset);
    }

    @Override
    public String toString() {
        return "CircularByteBuffer {r=" + this.data.getInt(READ) +
                ", w=" +
                this.data.getInt(WRITE) +
                ", data=" +
                IntStream.range(READ, this.data.array().length)
                        .map(x -> this.data.array()[x])
                        .mapToObj(Integer::toString)
                        .collect(Collectors.joining(",", "[", "]")) +
                "}";
    }
}
