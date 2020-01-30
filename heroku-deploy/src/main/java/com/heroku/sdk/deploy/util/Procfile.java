package com.heroku.sdk.deploy.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Procfile {
    private final Map<String, String> entries;

    public Procfile(Map<String, String> entries) {
        this.entries = new HashMap<>(entries);
    }

    public void add(String processType, String command) {
        entries.put(processType, command);
    }

    public boolean isEmpty(){
        return entries.isEmpty();
    }

    public Procfile merge(Procfile other) {
        Map<String, String> merged = new HashMap<>(entries);
        merged.putAll(other.entries);

        return new Procfile(merged);
    }

    public String asString() {
        StringBuilder stringBuilder = new StringBuilder();
        entries.forEach((processType, command) -> {
            stringBuilder.append(processType);
            stringBuilder.append(": ");
            stringBuilder.append(command);
            stringBuilder.append("\n");
        });

        return stringBuilder.toString();
    }

    public static Procfile empty() {
        return new Procfile(Collections.emptyMap());
    }

    public static Procfile singleton(String processType, String command) {
        return new Procfile(Collections.singletonMap(processType, command));
    }

    /**
     * Reads Profile from a file. If the file cannot be found, an empty Procfile is returned.
     */
    public static Procfile fromFile(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return new Procfile(new HashMap<>());
        }

        Map<String, String> entries = Files.readAllLines(path)
                .stream()
                .map(Procfile::parseLine)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Tuple::getA, Tuple::getB));

        return new Procfile(entries);
    }

    private static Optional<Tuple<String, String>> parseLine(String line) {
        if (line.contains(":")) {
            int index = line.indexOf(":");
            String key = line.substring(0, index).trim();
            String value = line.substring(index + 1).trim();

            if (key.isEmpty() || value.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new Tuple<>(key.trim(), value.trim()));
        }

        return Optional.empty();
    }
}
