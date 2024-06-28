package com.github.shautvast.exceptional;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enables multithreaded writing, while keeping CircularByteBuffer simpler (only suitable for single-threaded writing)
 */
public class MPSCBufferWriter implements AutoCloseable {

    private static final ConcurrentLinkedDeque<byte[]> writeQueue = new ConcurrentLinkedDeque<>(); // unbounded
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final CircularByteBuffer buffer;

    public MPSCBufferWriter(CircularByteBuffer buffer) {
        this.buffer = buffer;
        startWriteQueueListener();
    }

    private void startWriteQueueListener() {
        active.set(true);
        executorService.submit(() -> {
            while (active.get()) {
                var element = writeQueue.pollFirst();
                if (element != null) {
                    while (!buffer.put(element) && active.get()) {
                        Thread.yield();
                    }
                }
            }

        });
    }

    public void put(byte[] bytes) {
        writeQueue.offerLast(bytes);
    }

    /**
     * Shuts down the background thread
     */
    @Override
    public void close() {
        active.set(false);
        executorService.close();
    }
}
