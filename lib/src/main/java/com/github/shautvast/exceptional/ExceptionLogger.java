package com.github.shautvast.exceptional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unused") // this code is called from the instrumented code
public class ExceptionLogger {
    private final static ObjectMapper objectMapper = new ObjectMapper();
    private final static MPSCBufferWriter bufferWriter = new MPSCBufferWriter();

    public static void log(Throwable throwable) {
        try {
            // use json for now because of ease of integration
            // would compression be useful?? use snappy?
            if (throwable != null) {
                bufferWriter.put(objectMapper.writeValueAsBytes(throwable));
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }
}
