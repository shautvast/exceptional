package com.github.shautvast.exceptional;

import java.util.Arrays;

@SuppressWarnings("unused")
public class ThrowableHandler {
    public static void handle(Throwable throwable) {
        System.out.print("Handling exception:");
        Arrays.stream(throwable.getStackTrace()).forEach(System.out::println);
    }
}
