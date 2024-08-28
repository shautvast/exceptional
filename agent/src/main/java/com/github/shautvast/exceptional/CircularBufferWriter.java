package com.github.shautvast.exceptional;

import java.lang.foreign.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enables multithreaded writing, while keeping CircularByteBuffer simpler (only suitable for single-threaded writing)
 */
public class CircularBufferWriter implements AutoCloseable {
    private static Linker linker;
    private static SymbolLookup rustlib;
    private static final LinkedBlockingDeque<byte[]> writeQueue = new LinkedBlockingDeque<>(); // unbounded
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean active = new AtomicBoolean(false);

    public CircularBufferWriter() {
        startWriteQueueListener();
    }

    private void startWriteQueueListener() {
        active.set(true);

        executorService.submit(() -> {
            // maybe test again with this part of the code somewhere else. Did have issues when setting this up in the main thread, but need to investigate.

            // setup of native memory ringbuffer
            var arena = Arena.ofConfined();
            var ringbufferMemory = arena.allocate(32768);
            var buffer = new SingleThreadCircularByteBuffer(ringbufferMemory);

            arena = Arena.ofConfined();
            linker = Linker.nativeLinker();
            //TODO relative path, or configurable
            String agentlibPath = System.getProperty("agentlib");
            if (agentlibPath == null) {
                System.err.println("Please specify an agent library with -Dagentlib=<path to native agent>");
                System.exit(-1);
            }
            rustlib = SymbolLookup.libraryLookup(agentlibPath, arena);
            MemorySegment create = rustlib.find("buffer_updated").orElseThrow();
            var updateHandle = linker.downcallHandle(create, FunctionDescriptor.ofVoid(
                    ValueLayout.ADDRESS
            ));

            // start polling from the queue and offer elements to the ringbuffer
            byte[] element;
            while (active.get()) {
                try {
                    element = writeQueue.takeFirst(); // blocking read ie. efficient wait loop
                    while (!buffer.put(element) && active.get())
                        ; // busy loop supposed to be just 1 iteration, also depends on load and buffer size (TBD)

                    // once the buffer is updated we can signal an update to the rust lib, so it will read the next element
                    updateHandle.invoke(ringbufferMemory); // the update call is not supposed to cause a lot of overhead (TBD)
                    // and this setup prevents thread sync issues and unnecessary waits
                    //
                    // the memory is allocated only once, we just pass the pointer every time
                    // that is the simplest way on the rust side
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            System.out.println("Shutting down");

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
