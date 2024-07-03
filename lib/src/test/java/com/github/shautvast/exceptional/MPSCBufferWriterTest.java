package com.github.shautvast.exceptional;

import java.lang.foreign.Arena;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class MPSCBufferWriterTest {

//    @Test
    void test() throws InterruptedException {
        var arena = Arena.ofConfined();
        var ringbufferMemory = arena.allocate(4096);
//        var buffer = new CircularByteBuffer(ringbufferMemory);
        CircularBufferWriter writer = new CircularBufferWriter();
        byte[] bytes = "cow".getBytes(UTF_8);
        writer.put(bytes);
        writer.put(bytes);
        Thread.sleep(10000);
        writer.close();
    }
}