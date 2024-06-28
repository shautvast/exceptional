package com.github.shautvast.exceptional;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class MPSCBufferWriterTest {

    @Test
    void test() {
        CircularByteBuffer buffer = new CircularByteBuffer(9);
        try (MPSCBufferWriter writer = new MPSCBufferWriter(buffer)) {
            byte[] bytes = "cow".getBytes(UTF_8);
            writer.put(bytes);
            writer.put(bytes);
        }
    }
}