package com.heroku.deployer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
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

    public static boolean runIsCnb(Path workingDirectory, Optional<String> appName) throws IOException {
        List<String> commandArguments = new ArrayList<>();
        commandArguments.add("info");
        commandArguments.add("--shell");

        if (appName.isPresent()) {
            commandArguments.add("--app");
            commandArguments.add(appName.get());
        }

        List<String> lines = runRaw(workingDirectory, commandArguments.toArray(new String[0]));
        for (String line : lines) {
            if (line.equals("stack=cnb")) {
                return true;
            }
        }

        return false;
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
