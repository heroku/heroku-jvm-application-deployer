package com.heroku.deployer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class HerokuCli {

    public static Optional<String> runAuthToken(Path workingDirectory) throws IOException {
        List<String> lines = runRaw(workingDirectory,"auth:token");

        if (lines.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(lines.get(0));
        }
    }

    private static List<String> runRaw(Path workingDirectory, String... command) throws IOException {
        List<String> fullCommand =  new ArrayList<>(Arrays.asList(command));
        fullCommand.add(0, "heroku");

        ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
        processBuilder.directory(workingDirectory.toFile());
        Process process = processBuilder.start();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            return bufferedReader.lines().collect(Collectors.toList());
        }
    }
}
