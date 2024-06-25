package com.github.shautvast.exceptional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionLoggerTest {

    @Test
    void test(){
        ExceptionLogger.log(new Throwable());
    }

}