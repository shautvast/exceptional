package com.github.shautvast.exceptional;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Single-threaded Circular buffer for variable sized byte arrays. The indices for read and write
 * are also stored in the bytebuffer, making changes visible to any non-java process that is reading.
 *
 *
 * Written for a scenario with multiple concurrent writers, and a single reader in a non-java process
 * This class relies on MPSCBufferWriter for multithreaded writes. This class queues
 * byte arrays waiting to be stored in the circular buffer. Effectively MPSCBufferWriter starts the only
 * thread that is allowed to interact with the CircularByteBuffer.
 * ..
 * *Implementation note:*
 * The last 8 bytes are reserved for the reader and writer index. The actual capacity is always `bytebuffer.capacity -8`
 */
@SuppressWarnings("StringTemplateMigration")
public class SingleThreadCircularByteBuffer {

    private int readStartPos;
    private int writeStartPos;
    private int capacity;
    final ByteBuffer data;

    /**
     * Constructs a CircularByteBuffer with the specified capacity.
     * The buffer is backed by a byte array on the java-heap. Mainly there for test purposes.
     *
     * @param capacity the capacity of the CircularByteBuffer
     */
    public SingleThreadCircularByteBuffer(int capacity) {
        this.data = ByteBuffer.allocate(capacity + 8); // 8 extra for the read and write index
        initIndices();
    }

    /**
     * Constructs a CircularByteBuffer with the specified capacity. The buffer is backed by native memory
     * from the MemorySegment
     */
    public SingleThreadCircularByteBuffer(MemorySegment memory) {
        if (memory.byteSize() > 0xfff7) {
            throw new IllegalArgumentException("Max memory size is 65527");
        }
        this.data = memory.asByteBuffer();
        initIndices();
    }

    private void initIndices() {
        this.capacity = this.data.capacity() - 8;
        readStartPos = this.capacity; // write values after logical capacity position
        writeStartPos = this.capacity + 4;

        this.data.putInt(readStartPos, 0);
        this.data.putInt(writeStartPos, 0);
    }

    public boolean put(byte[] bytes) {
        int len = bytes.length;
        int remaining;
        // check capacity for bytes to insert
        int readIndex = getReadIndex();
        int writeIndex = getWriteIndex();
        try {
            if (writeIndex >= readIndex) {
                remaining = capacity - writeIndex + readIndex;
            } else {
                remaining = readIndex - writeIndex;
            }
            if (remaining < len + 2) {
                return false;
            } else {
                int remainingUntilEnd = capacity - writeIndex;
                if (remainingUntilEnd < len + 2) {
                    if (remainingUntilEnd > 1) {
                        // we can write the length
                        this.data.putShort(writeIndex, (short) len);
                        writeIndex += 2;
                        remainingUntilEnd -= 2;
                        if (remainingUntilEnd > 0) {
                            this.data.put(writeIndex, bytes, 0, remainingUntilEnd);
                        }
                        writeIndex = 0;
                        this.data.put(writeIndex, bytes, remainingUntilEnd, len - remainingUntilEnd);
                        writeIndex += len - remainingUntilEnd;
                    } else {
                        // we can write only one byte of the length
                        this.data.put(writeIndex, (byte) (len >> 8));
                        writeIndex = 0;
                        this.data.put(writeIndex, (byte) (len & 0xff));
                        writeIndex += 1;
                        this.data.put(writeIndex, bytes);
                        writeIndex += len;
                    }
                } else {
                    this.data.putShort(writeIndex, (short) len);
                    writeIndex += 2;
                    this.data.put(writeIndex, bytes);
                    writeIndex += len;

                    if (writeIndex == this.capacity) {
                        writeIndex = 0;
                    }
                }

                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            setWriteIndex(writeIndex);
        }
    }

    /**
     * The reader side is provided, for reference and testability only.
     * In practice, the reader is implemented outside of java, see rustlib module
     */
    public byte[] get() {
        int readIndex = getReadIndex();
        int writeIndex = getWriteIndex();
        if (readIndex == writeIndex) {
            return null;
        }

        try {
            int remainingUntilEnd = capacity - readIndex;
            int len;
            if (remainingUntilEnd == 1) {
                byte high = this.data.get(readIndex);
                readIndex = 0;
                byte low = this.data.get(readIndex);
                readIndex += 1;
                len = high << 8 | low;
                remainingUntilEnd = len;
            } else if (remainingUntilEnd == 2) {
                len = this.data.getShort(readIndex);
                readIndex = 0;
                remainingUntilEnd = 0;
            } else {
                len = this.data.getShort(readIndex);
                readIndex += 2;
                remainingUntilEnd -= 2;
            }
            byte[] result = new byte[len];
            if (len <= remainingUntilEnd) {
                this.data.get(readIndex, result);
                readIndex += len;
            } else {
                this.data.get(readIndex, result, 0, remainingUntilEnd);
                readIndex = 0;
                this.data.get(readIndex, result, remainingUntilEnd, len - remainingUntilEnd);
                readIndex += len - remainingUntilEnd;
            }
            return result;
        } finally {
            setReadIndex(readIndex);
        }
    }

    int getWriteIndex() {
        return this.data.getInt(writeStartPos);
    }

    void setWriteIndex(int writeIndex) {
        this.data.putInt(writeStartPos, writeIndex);
    }

    int getReadIndex() {
        return this.data.getInt(readStartPos);
    }

    void setReadIndex(int readIndex) {
        this.data.putInt(readStartPos, readIndex);
    }

    @Override
    public String toString() {
        return "CircularByteBuffer {r=" + this.data.getInt(readStartPos) +
                ", w=" +
                this.data.getInt(writeStartPos) +
                ", data=" +
                bytesToString(this.data.array()) +
                "}";
    }

    public static String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        return IntStream.range(0, bytes.length)
                .map(x -> bytes[x])
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }


}
