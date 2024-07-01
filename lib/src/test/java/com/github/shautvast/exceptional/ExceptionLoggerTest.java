package com.github.shautvast.exceptional;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class ExceptionLoggerTest {

    @Test
    void test() throws InterruptedException {
        ExceptionLogger.log(new Throwable());
        TimeUnit.SECONDS.sleep(30);
    }

}