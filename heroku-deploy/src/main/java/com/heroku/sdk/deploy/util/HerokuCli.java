package com.heroku.sdk.deploy.util;

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
        return Optional.ofNullable(runRaw(workingDirectory,"auth:token").get(0));
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
