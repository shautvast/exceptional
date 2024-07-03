package com.github.shautvast.exceptional;

import org.junit.jupiter.api.Test;

class ExceptionLoggerTest {

    @Test
    void test() {
        ExceptionLogger.log(new Throwable());
    }

}