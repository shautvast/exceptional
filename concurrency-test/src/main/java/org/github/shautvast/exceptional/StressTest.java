package org.github.shautvast.exceptional;

import com.github.shautvast.exceptional.CircularByteBuffer;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.concurrent.atomic.AtomicInteger;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

/**
 * The circular buffer is put under stress to see if it fails basic expectations (what goes in
 * must come out
 */
@JCStressTest
@Outcome(id = {"0", "1", "2147483647"}, expect = ACCEPTABLE, desc = "Value is correct, or not set")
@State
public class StressTest {
    private final CircularByteBuffer buffer = new CircularByteBuffer(100);
    private final AtomicInteger counter = new AtomicInteger(0);

    @Actor
    public void actor1(I_Result r) {
        buffer.put(toBigEndianByteArray(counter.getAndSet(1)));
        byte[] bytes = buffer.get();
        if (bytes != null) {
            r.r1 = fromBigEndianByteArray(bytes);
        }
    }

    @Actor
    public void actor2(I_Result r) {
        buffer.put(toBigEndianByteArray(counter.getAndSet(Integer.MAX_VALUE)));
        byte[] bytes = buffer.get();
        if (bytes != null) {
            r.r1 = fromBigEndianByteArray(bytes);
        }
    }

    private static byte[] toBigEndianByteArray(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }

    private static int fromBigEndianByteArray(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                (bytes[3] & 0xFF);
    }
}