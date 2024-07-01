package com.github.shautvast.exceptional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.foreign.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enables multithreaded writing, while keeping CircularByteBuffer simpler (only suitable for single-threaded writing)
 */
public class MPSCBufferWriter implements AutoCloseable {
    private static  Linker linker;
    private static  SymbolLookup rustlib;
    private static final ConcurrentLinkedDeque<byte[]> writeQueue = new ConcurrentLinkedDeque<>(); // unbounded
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final AtomicBoolean active = new AtomicBoolean(false);

    public MPSCBufferWriter() {
        startWriteQueueListener();
    }

    private void startWriteQueueListener() {
        active.set(true);

        executorService.submit(() -> {
            // maybe test again with this part of the code somewhere else

            // setup of native memory ringbuffer
            var arena = Arena.ofConfined();
            var ringbufferMemory = arena.allocate(32768);
            var buffer = new CircularByteBuffer(ringbufferMemory);

            arena = Arena.ofConfined();
            linker = Linker.nativeLinker();
            //TODO relative path, or configurable
            rustlib = SymbolLookup.libraryLookup("/Users/Shautvast/dev/exceptional/rustlib/target/debug/librustlib.dylib", arena);
            MemorySegment create = rustlib.find("create_ring_buffer").orElseThrow();
            var createHandle = linker.downcallHandle(create, FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS
            ));
            try {
                createHandle.invoke(ringbufferMemory);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            // start polling from the queue and offer elements to the ringbuffer
            while (active.get()) {
                var element = writeQueue.pollFirst();
                if (element != null) {
                    while (!buffer.put(element) && active.get()) {
                        try {
                            Thread.sleep(1); // TODO remove the sleep
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
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
