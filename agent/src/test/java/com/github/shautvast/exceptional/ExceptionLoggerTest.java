package com.github.shautvast.exceptional;

import org.junit.jupiter.api.Test;

import java.time.Duration;

class ExceptionLoggerTest {

    @Test
    void test() throws InterruptedException {
        for (int i = 0; i < 1; i++) {
            ExceptionLogger.log(new Throwable());
        }
        Thread.sleep(Duration.ofSeconds(1));
    }

}