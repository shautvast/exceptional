package com.github.shautvast.exceptional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

@SuppressWarnings("unused") // this code is called from the instrumented code
public class ExceptionLogger {
    private static final Arena arena = Arena.ofConfined();
    private static final Linker linker = Linker.nativeLinker();
    //    //TODO relative path, or configurable
    private static final SymbolLookup rustlib = SymbolLookup.libraryLookup("/Users/Shautvast/dev/exceptional/rustlib/target/debug/librustlib.dylib", arena);
    private final static MethodHandle logNative;
    private final static ObjectMapper objectMapper = new ObjectMapper();

    static {
        MemorySegment logFunction = rustlib.find("log_java_exception").orElseThrow();
        logNative = linker.downcallHandle(logFunction, FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS
        ));
    }

    // how does this behave in a multithreaded context??
    // probably need a ringbuffer with fixed memory to make this work efficiently
    public static void log(Throwable throwable) {
        try {
            // use json for now because of ease of integration
            if (throwable != null) {
                String json = objectMapper.writeValueAsString(throwable);
                var data = arena.allocateFrom(json); // reuse instead of reallocating?
                logNative.invoke(data); // invoke the rust function
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }
}
