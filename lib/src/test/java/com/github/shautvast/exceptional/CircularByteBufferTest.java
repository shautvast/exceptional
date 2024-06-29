package com.github.shautvast.exceptional;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class CircularByteBufferTest {

    @Test
    void testPutAndGet() {
        CircularByteBuffer buffer = new CircularByteBuffer(9);
        byte[] bytes = "hello".getBytes(UTF_8);
        boolean written = buffer.put(bytes);
        assertTrue(written);
        assertArrayEquals(bytes, buffer.get());
        assertArrayEquals(new byte[]{0, 0, 0, 15, 0, 0, 0, 15, 0, 5, 104, 101, 108, 108, 111, 0, 0}, buffer.data.array());
    }


    @Test
    void testPutFitsBeforeGet() {
        CircularByteBuffer buffer = new CircularByteBuffer(14);
        byte[] bytes = "hello".getBytes(UTF_8);
        buffer.setWriteIndex(7);
        buffer.setReadIndex(7);
        buffer.put(bytes);
        assertArrayEquals(new byte[]{0, 0, 0, 15, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 5, 104, 101, 108, 108, 111}, buffer.data.array());
//        buffer.setWriteIndex(0);
        // end of setup, situation where writeIndex < readIndex
        boolean written = buffer.put(bytes);
        assertTrue(written);
        assertArrayEquals(new byte[]{0, 0, 0, 15, 0, 0, 0, 15, 0, 5, 104, 101, 108, 108, 111, 0, 5, 104, 101, 108, 108, 111}, buffer.data.array());
        assertEquals(7, buffer.getReadIndex());
        assertEquals(7, buffer.getWriteIndex());
    }

    @Test
    void testPutFitsNotBeforeGet() {
        CircularByteBuffer buffer = new CircularByteBuffer(13);
        byte[] bytes = "hello".getBytes(UTF_8);
        buffer.setWriteIndex(6);
        buffer.setReadIndex(6);
        buffer.put(bytes);
        assertArrayEquals(new byte[]{0, 0, 0, 14, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 5, 104, 101, 108, 108, 111}, buffer.data.array());

        // end of setup, situation where writeIndex < readIndex
        boolean written = buffer.put(bytes);
        assertFalse(written);
        assertArrayEquals(new byte[]{0, 0, 0, 14, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 5, 104, 101, 108, 108, 111}, buffer.data.array());
    }

    @Test
    void testWrapAroundPutLenAndOneCharBeforeWrap() {
        CircularByteBuffer buffer = new CircularByteBuffer(9);
        byte[] bytes = "hello".getBytes(UTF_8);
        buffer.setWriteIndex(6);
        buffer.setReadIndex(6);
        boolean written = buffer.put(bytes);
        assertTrue(written);
        assertArrayEquals(new byte[]{0, 0, 0, 14, 0, 0, 0, 12, 101, 108, 108, 111, 0, 0, 0, 5, 104}, buffer.data.array());
        assertArrayEquals(bytes, buffer.get());
    }

    @Test
    void testWrapAroundPutLenBeforeWrap() {
        CircularByteBuffer buffer = new CircularByteBuffer(9);
        byte[] bytes = "hello".getBytes(UTF_8);
        buffer.setWriteIndex(7);
        buffer.setReadIndex(7);
        boolean written = buffer.put(bytes);
        assertTrue(written);
        assertArrayEquals(new byte[]{0, 0, 0, 15, 0, 0, 0, 13, 104, 101, 108, 108, 111, 0, 0, 0, 5}, buffer.data.array());
        assertArrayEquals(bytes, buffer.get());
    }

    @Test
    void testWrapAroundPutLenSplitBeforeWrap() {
        CircularByteBuffer buffer = new CircularByteBuffer(9);
        byte[] bytes = "hello".getBytes(UTF_8);
        buffer.setWriteIndex(8);
        buffer.setReadIndex(8);
        boolean written = buffer.put(bytes);
        assertTrue(written);
        assertArrayEquals(new byte[]{0, 0, 0, 16, 0, 0, 0, 14, 5, 104, 101, 108, 108, 111, 0, 0, 0}, buffer.data.array());
        assertArrayEquals(bytes, buffer.get());
    }

    @Test
    void testNoFreeSpace() {
        CircularByteBuffer buffer = new CircularByteBuffer(9);
        byte[] bytes = "hello".getBytes(UTF_8);
        boolean written1 = buffer.put(bytes);
        assertTrue(written1);
        boolean written2 = buffer.put(bytes);
        assertFalse(written2); // no space left
    }

    @Test
    void testFreeSpaceReclaimed() {
        CircularByteBuffer buffer = new CircularByteBuffer(9);
        assertEquals(0, buffer.getReadIndex());
        assertEquals(0, buffer.getWriteIndex());

        byte[] bytes = "hello".getBytes(UTF_8);
        boolean written1 = buffer.put(bytes);
        assertTrue(written1);
        assertEquals(0, buffer.getReadIndex());
        assertEquals(7, buffer.getWriteIndex());

        assertArrayEquals(bytes, buffer.get());
        assertEquals(7, buffer.getReadIndex());
        assertEquals(7, buffer.getWriteIndex());

        boolean written2 = buffer.put(bytes);
        assertTrue(written2); // the read has freed space
        assertEquals(7, buffer.getReadIndex());
        assertEquals(5, buffer.getWriteIndex());


        assertArrayEquals(bytes, buffer.get());
        assertEquals(5, buffer.getReadIndex());
        assertEquals(5, buffer.getWriteIndex());
    }


}