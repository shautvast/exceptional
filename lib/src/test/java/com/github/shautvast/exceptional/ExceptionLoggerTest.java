package com.github.shautvast.exceptional;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class ExceptionLoggerTest {

    @Test
    void test() throws InterruptedException {
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 1_000; i++) {
            ExceptionLogger.log(new Throwable());
            TimeUnit.MILLISECONDS.sleep(1);
        }
        System.out.println(System.currentTimeMillis() - t0);
        Thread.sleep(10000);
    }

}