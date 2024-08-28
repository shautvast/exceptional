package com.github.shautvast.exceptional;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Circular bytebuffer for variable length entries, that allows concurrent writing, using CAS.
 * Multi threaded reading is provided, but:
 * - mainly for testing the reliability of the buffer
 * - relies on a ReentrantLock, preventing multiple reads at the same read index
 */
public class CircularByteBuffer {

    private final AtomicLong readPosition;
    private final AtomicLong writePosition;
    final ByteBuffer data;
    private final int capacity;
    private final int readStartPos;
    private final int writeStartPos;
    private final Lock readLock = new ReentrantLock();

    // TODO implement MemorySegment as backing buffer
    public CircularByteBuffer(int capacity) {
        if (capacity > Integer.MAX_VALUE - 8 || capacity < 0) {
            throw new IllegalArgumentException("illegal capacity: " + capacity);
        }
        this.capacity = capacity; //logical cap is bytebuffer cap - 8. extra space for the read and write pointers so that non-java processes can read it there
        this.data = ByteBuffer.allocate(capacity + 8);

        readStartPos = capacity; // write values after logical capacity position
        writeStartPos = capacity + 4;

        this.readPosition = new AtomicLong();
        this.writePosition = new AtomicLong();
        setReadIndex(0);
        setWriteIndex(0);
    }

    public boolean put(byte[] bytes) {
        boolean updatePending = true;
        while (updatePending) {
            int oldwriteIndex = writePosition.intValue();
            int writeIndex = oldwriteIndex;
            int len = bytes.length;
            int remaining;
            // check capacity for bytes to insert
            int readIndex = readPosition.intValue();
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
                    if (writePosition.compareAndSet(oldwriteIndex, writeIndex)) {
                        this.data.putInt(writeStartPos, writePosition.intValue());
                        updatePending = false;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    /**
     * The reader side is provided, for reference and testability only.
     * In practice, the reader is implemented outside of java, see rustlib module
     */
    public byte[] get() {
        readLock.lock();
        int readIndex = readPosition.intValue();
        int writeIndex = writePosition.intValue();
        if (readIndex == writeIndex) {
            return null;
        }

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
            setReadIndex(readIndex + len);
            readLock.unlock();
            this.data.get(readIndex, result);
        } else {
            setReadIndex(len - remainingUntilEnd);
            readLock.unlock();
            this.data.get(readIndex, result, 0, remainingUntilEnd);
            this.data.get(0, result, remainingUntilEnd, len - remainingUntilEnd);
        }
        return result;

    }


    void setWriteIndex(int i) {
        writePosition.set(i);
        this.data.putInt(writeStartPos, writePosition.intValue());
    }

    void setReadIndex(int i) {
        readPosition.set(i);
        this.data.putInt(readStartPos, readPosition.intValue());
    }

    int getReadIndex() {
        return readPosition.intValue();
    }

    int getWriteIndex() {
        return writePosition.intValue();
    }
}
