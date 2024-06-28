package com.github.shautvast.exceptional;

import java.nio.ByteBuffer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Circular buffer for variable sized byte arrays
 * The singlethread version
 */
@SuppressWarnings("StringTemplateMigration")
public class CircularByteBuffer {

    final ByteBuffer data;
    int readIndex = 0;
    int writeIndex = 0;

    public CircularByteBuffer(int capacity) {
        data = ByteBuffer.allocate(capacity);
    }

    public boolean put(byte[] bytes) {
        int len = bytes.length;
        int remaining;
        // check capacity for bytes to insert
        if (writeIndex >= readIndex) {
            remaining = data.capacity() - writeIndex + readIndex;
        } else {
            remaining = readIndex - writeIndex;
        }
        if (remaining < len + 2) {
            return false;
        } else {
            int remainingUntilEnd = data.capacity() - writeIndex;
            if (remainingUntilEnd < len + 2) {
                if (remainingUntilEnd > 1) {
                    // we can write the length
                    data.putShort(writeIndex, (short) len);
                    writeIndex += 2;
                    remainingUntilEnd -= 2;
                    if (remainingUntilEnd > 0) {
                        data.put(writeIndex, bytes, 0, remainingUntilEnd);
                    }
                    writeIndex = 0;
                    data.put(writeIndex, bytes, remainingUntilEnd, len - remainingUntilEnd);
                    writeIndex += len - remainingUntilEnd;
                } else {
                    // we can write only one byte of the length
                    data.put(writeIndex, (byte) (len >> 8));
                    writeIndex = 0;
                    data.put(writeIndex, (byte) (len & 0xff));
                    writeIndex += 1;
                    data.put(writeIndex, bytes);
                    writeIndex += len;
                }
            } else {
                data.putShort(writeIndex, (short) len);
                writeIndex += 2;
                data.put(writeIndex, bytes);
                writeIndex += len;
            }
            return true;
        }
    }

    public byte[] get() {
        int remainingUntilEnd = data.capacity() - readIndex;
        int len;
        if (remainingUntilEnd == 1) {
            byte high = data.get(readIndex);
            readIndex = 0;
            byte low = data.get(readIndex);
            readIndex = 1;
            len = high << 8 | low;
            remainingUntilEnd = len;
        } else if (remainingUntilEnd == 2) {
            len = data.getShort(readIndex);
            readIndex = 0;
            remainingUntilEnd = 0;
        } else {
            len = data.getShort(readIndex);
            readIndex += 2;
            remainingUntilEnd -= 2;
        }
        byte[] result = new byte[len];
        if (len <= remainingUntilEnd) {
            data.get(readIndex, result);
            readIndex += len;
        } else {
            data.get(readIndex, result, 0, remainingUntilEnd);
            readIndex = 0;
            data.get(readIndex, result, remainingUntilEnd, len - remainingUntilEnd);
            readIndex += len - remainingUntilEnd;
        }
        return result;
    }

    @Override
    public String toString() {
        return "CircularBuffer {r=" + this.readIndex +
                ", w=" +
                this.writeIndex +
                ", data=" +
                IntStream.range(0, this.data.array().length)
                        .map(x -> this.data.array()[x])
                        .mapToObj(Integer::toString)
                        .collect(Collectors.joining(",", "[", "]")) +
                "}";
    }
}
