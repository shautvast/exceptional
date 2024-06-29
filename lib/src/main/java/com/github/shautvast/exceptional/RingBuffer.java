package com.github.shautvast.exceptional;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

//circular MPSC buffer
//TODO REMOVE
public class RingBuffer implements AutoCloseable {
    private final ByteBuffer memory;
    private final AtomicInteger readPointer;
    private final AtomicInteger writePointer;
    private final AtomicBoolean writerThreadRunning = new AtomicBoolean(true);
    private final AtomicBoolean readerThreadRunning = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();
    private static final LinkedBlockingDeque<byte[]> writeQueue = new LinkedBlockingDeque<>();
    private final ExecutorService writerThread;
    private ExecutorService readerThread;

    void setReadPointer(int readPointer) {
        this.readPointer.set(readPointer);
    }

    public RingBuffer(MemorySegment memory) {
        if (memory.byteSize() > 0xfffd) {
            throw new IllegalArgumentException("Max memory size is 65533"); //TODO probably ffff
        }
        this.memory = memory.asByteBuffer();
        memory.asByteBuffer();
        readPointer = new AtomicInteger(0);
        writePointer = new AtomicInteger(0);

        writerThread = Executors.newSingleThreadExecutor();
        writerThread.submit(() -> {
            while (writerThreadRunning.get()) {
                try {
                    byte[] data = writeQueue.poll(5, TimeUnit.SECONDS);
                    if (data != null) {
                        while (!writeBytes(data)) {
                            Thread.sleep(5000);
                        }
                        ;
                    }
                } catch (InterruptedException _) {
                    // honor the interrupt
                    writerThreadRunning.set(false);
                }
            }
        });

    }


    /**
     * Writes a byte array to the ring buffer.
     * <p>
     * If there is enough space in the buffer, the method writes the byte array to the buffer
     * and returns true. If there is not enough space, the method does not write the byte array
     * and returns false.
     *
     * @param data the byte array to write to the buffer
     * @return true if the byte array was written successfully, false otherwise
     */
    private boolean writeBytes(byte[] data) {
        if (writePointer.get() > memory.capacity()) {
            System.out.println("blocked");
            return false;//signal retry
        }
        System.out.println("write " + new String(data));
        int allocSize = data.length + 2;
        int pos = writePointer.getAndAdd(allocSize);
        if (writePointer.get() > (memory.capacity() - 2)) {
            int max = memory.capacity() - (pos + 4);
            if (data.length - max < readPointer.get()) {
                System.out.println("wrap");
                memory.putShort(pos, (short) data.length);
                memory.position(pos + 2);
                memory.put(data, 0, max);
                memory.position(0);
                memory.put(data, max, data.length - max);
                writePointer.set(data.length - max);
                memory.putShort((short) 0);
                return true;
            } else {
                return false;
            }
        } else {
            memory.putShort(pos, (short) data.length);
            memory.position(pos + 2);
            memory.put(data);
            memory.putShort((short) 0);
            return true;
        }
    }

    /**
     * Reads a byte array from the ring buffer with a specified timeout.
     * <p>
     * Blocks until there is data available to read or the timeout is reached.
     * If the timeout is reached and there is still no data, the resul is empty.
     *
     * @param timeout the maximum time to wait for data to be available in the buffer
     * @return the byte array read from the buffer
     * @throws InterruptedException if the thread is interrupted while waiting for data
     */
    private Optional<byte[]> read(Duration timeout) throws InterruptedException {
        if (memory.getShort(readPointer.get()) == 0 || readPointer.get() >= memory.capacity()) {
            return Optional.empty();
        }
        return Optional.ofNullable(getBytes());
    }

    private byte[] getBytes() {
        int currentReadPointerValue = readPointer.get();
        int lenToread = memory.getShort(currentReadPointerValue);
        System.out.println(lenToread + " bytes");
        if (lenToread <= 0) {
            return null;
        }
        currentReadPointerValue = readPointer.addAndGet(2);
        byte[] data = new byte[lenToread];
        int bytesTilEnd = memory.capacity() - currentReadPointerValue;
        if (lenToread > bytesTilEnd) {
            memory.get(currentReadPointerValue, data, 0, bytesTilEnd);
            memory.get(0, data, bytesTilEnd, lenToread - bytesTilEnd);
            readPointer.set(lenToread - bytesTilEnd);
        } else {
            memory.get(currentReadPointerValue, data);
            System.out.println("set "+readPointer.addAndGet(lenToread));
        }
        return data;
    }


    public void write(byte[] bytes) {
        while (!writeQueue.offer(bytes)) ;
    }

    public void startReader(Consumer<byte[]> consumer) {
        readerThreadRunning.set(true);
        readerThread = Executors.newSingleThreadExecutor();

        readerThread.submit(() -> {
            while (readerThreadRunning.get()) {
                try {
                    System.out.println("read");
                    read(Duration.ofSeconds(5)).ifPresent(consumer);
                    Thread.sleep(5000);
                } catch (InterruptedException _) {
                    readerThreadRunning.set(false);
                }
            }
        });

    }

    public void close() {
        System.out.println("close");
        writerThreadRunning.set(false);
        readerThreadRunning.set(false);
        writerThread.close();
        readerThread.close();
    }

    public void drain() {
        while (!writeQueue.isEmpty()) ;
        close();
    }
}

