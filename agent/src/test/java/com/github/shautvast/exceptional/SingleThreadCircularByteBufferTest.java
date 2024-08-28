package com.github.shautvast.exceptional;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class SingleThreadCircularByteBufferTest {

    @Test
    void testPutAndGet() {
        var buffer = new SingleThreadCircularByteBuffer(9);
        byte[] bytes = "hello".getBytes(UTF_8);
        boolean written = buffer.put(bytes);
        assertTrue(written);
        assertArrayEquals(bytes, buffer.get());
        assertArrayEquals(new byte[]{0, 5, 104, 101, 108, 108, 111, 0, 0, 0, 0, 0, 7, 0, 0, 0, 7}, buffer.data.array());
    }

    @Test
    void testJustGet() {
        var buffer = new SingleThreadCircularByteBuffer(8);
        System.out.println(SingleThreadCircularByteBuffer.bytesToString(buffer.get()));
    }


    @Test
    void testPutFitsBeforeGet() {
        var buffer = new SingleThreadCircularByteBuffer(14);
        var bytes = "hello".getBytes(UTF_8);
        buffer.setWriteIndex(7);
        buffer.setReadIndex(7);
        buffer.put(bytes);
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 5, 104, 101, 108, 108, 111, 0, 0, 0, 7, 0, 0, 0, 0}, buffer.data.array());
//        buffer.setWriteIndex(0);
        // end of setup, situation where writeIndex < readIndex
        var written = buffer.put(bytes);
        assertTrue(written);
        assertArrayEquals(new byte[]{0, 5, 104, 101, 108, 108, 111, 0, 5, 104, 101, 108, 108, 111, 0, 0, 0, 7, 0, 0, 0, 7}, buffer.data.array());
        assertEquals(7, buffer.getReadIndex());
        assertEquals(7, buffer.getWriteIndex());
    }

    @Test
    void testPutFitsNotBeforeGet() {
        var buffer = new SingleThreadCircularByteBuffer(13);
        var bytes = "hello".getBytes(UTF_8);
        buffer.setWriteIndex(6);
        buffer.setReadIndex(6);
        buffer.put(bytes);
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 5, 104, 101, 108, 108, 111, 0, 0, 0, 6, 0, 0, 0, 0}, buffer.data.array());

        // end of setup, situation where writeIndex < readIndex
        boolean written = buffer.put(bytes);
        assertFalse(written);
        assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 5, 104, 101, 108, 108, 111, 0, 0, 0, 6, 0, 0, 0, 0}, buffer.data.array());
    }

    @Test
    void testWrapAroundPutLenAndOneCharBeforeWrap() {
        var buffer = new SingleThreadCircularByteBuffer(9);
        var bytes = "hello".getBytes(UTF_8);
        buffer.setWriteIndex(6);
        buffer.setReadIndex(6);
        boolean written = buffer.put(bytes);
        assertTrue(written);
        assertArrayEquals(new byte[]{101, 108, 108, 111, 0, 0, 0, 5, 104, 0, 0, 0, 6, 0, 0, 0, 4}, buffer.data.array());
        assertArrayEquals(bytes, buffer.get());
    }

    @Test
    void testWrapAroundPutLenBeforeWrap() {
        var buffer = new SingleThreadCircularByteBuffer(9);
        var bytes = "hello".getBytes(UTF_8);
        buffer.setWriteIndex(7);
        buffer.setReadIndex(7);
        var written = buffer.put(bytes);
        assertTrue(written);
        assertArrayEquals(new byte[]{104, 101, 108, 108, 111, 0, 0, 0, 5, 0, 0, 0, 7, 0, 0, 0, 5}, buffer.data.array());
        assertArrayEquals(bytes, buffer.get());
    }

    @Test
    void testWrapAroundPutLenSplitBeforeWrap() {
        var buffer = new SingleThreadCircularByteBuffer(9);
        var bytes = "hello".getBytes(UTF_8);
        buffer.setWriteIndex(8);
        buffer.setReadIndex(8);
        var written = buffer.put(bytes);
        assertTrue(written);
        assertArrayEquals(new byte[]{5, 104, 101, 108, 108, 111, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 6}, buffer.data.array());
        assertArrayEquals(bytes, buffer.get());
    }

    @Test
    void testNoFreeSpace() {
        var buffer = new SingleThreadCircularByteBuffer(9);
        var bytes = "hello".getBytes(UTF_8);
        boolean written1 = buffer.put(bytes);
        assertTrue(written1);
        boolean written2 = buffer.put(bytes);
        assertFalse(written2); // no space left
    }

    @Test
    void testFreeSpaceReclaimed() {
        var buffer = new SingleThreadCircularByteBuffer(9);
        assertEquals(0, buffer.getReadIndex());
        assertEquals(0, buffer.getWriteIndex());

        var bytes = "hello".getBytes(UTF_8);
        var written1 = buffer.put(bytes);
        assertTrue(written1);
        assertEquals(0, buffer.getReadIndex());
        assertEquals(7, buffer.getWriteIndex());

        assertArrayEquals(bytes, buffer.get());
        assertEquals(7, buffer.getReadIndex());
        assertEquals(7, buffer.getWriteIndex());

        var written2 = buffer.put(bytes);
        assertTrue(written2); // the read has freed space
        assertEquals(7, buffer.getReadIndex());
        assertEquals(5, buffer.getWriteIndex());


        assertArrayEquals(bytes, buffer.get());
        assertEquals(5, buffer.getReadIndex());
        assertEquals(5, buffer.getWriteIndex());
    }


}