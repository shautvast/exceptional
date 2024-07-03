package com.github.shautvast.exceptional;


import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

// TODO scheduled for demolition
class RingBufferTest {

    @Test
    void testWriteAndRead() {
//        var ringBuffer = new CircularByteBuffer(MemorySegment.ofArray(new byte[16]));
        var writer = new CircularBufferWriter();

//        writer.startReader(x -> System.out.println("read " + new String(x, StandardCharsets.UTF_8)));
        for (int i = 0; i < 10; i++) {
            System.out.println("put " + i + " in ring buffer");
            byte[] testdata = ("test" + i).getBytes(StandardCharsets.UTF_8);
            writer.put(testdata);
        }

    }
}
