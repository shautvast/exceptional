package com.github.shautvast.exceptional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.xerial.snappy.Snappy;

@SuppressWarnings("unused") // this code is called from the instrumented code
public class ExceptionLogger {
    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final static CircularBufferWriter bufferWriter = new CircularBufferWriter();

    public static void log(Throwable throwable) {
        try {
            // use json for now because of ease of integration
            // would compression be useful?? use snappy?
            if (throwable != null) {
                bufferWriter.put(Snappy.compress(objectMapper.writeValueAsBytes(throwable)));
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }
}
