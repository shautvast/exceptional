package com.github.shautvast.exceptional;

import org.junit.jupiter.api.Test;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class Compress {

    @Test
    void compress() throws IOException {
        byte[] helloWorld = Snappy.compress("{\"cause\":null,\"stackTrace\":[{\"classLoaderName\":\"app\",\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"call\",\"fileName\":\"Main.java\",\"lineNumber\":17,\"className\":\"Main\",\"nativeMethod\":false},{\"classLoaderName\":\"app\",\"moduleName\":null,\"moduleVersion\":null,\"methodName\":\"main\",\"fileName\":\"Main.java\",\"lineNumber\":7,\"className\":\"Main\",\"nativeMethod\":false}],\"message\":\"5778\",\"suppressed\":[],\"localizedMessage\":\"5778\"}");
        Files.write(Paths.get("hello.snap"), helloWorld);

    }
}
