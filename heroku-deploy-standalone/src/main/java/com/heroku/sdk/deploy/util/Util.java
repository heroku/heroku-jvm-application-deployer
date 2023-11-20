package com.heroku.sdk.deploy.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class Util {
    public static Stream<String> readLinesFromInputStream(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        try {
            return bufferedReader.lines();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
