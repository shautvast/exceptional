package com.github.shautvast.exceptional;

import org.junit.jupiter.api.Test;

class ExceptionLoggerTest {

//    @Test // needs -Dagentlib=$PROJECT/rustlib/target/debug/librustlib.dylib, or similar
    void test() {
        ExceptionLogger.log(new Throwable());
    }

}