package com.github.shautvast.exceptional;

import java.util.Arrays;

@SuppressWarnings("unused") // this code is called from the instrumented code
public class ExceptionLogger {
    public static void log(Throwable throwable) {
        System.out.print("Logging exception:");
        Arrays.stream(throwable.getStackTrace()).forEach(System.out::println);
    }
}
