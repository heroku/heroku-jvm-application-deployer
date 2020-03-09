package com.heroku.sdk.deploy.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PathUtils {

    public static List<Path> normalizeAll(Path basePath, List<Path> paths) {
        List<Path> normalizedPaths = new ArrayList<>();
        for (Path path : paths) {
            normalize(basePath, path).ifPresent(normalizedPaths::add);
        }

        return normalizedPaths;
    }

    public static Optional<Path> normalize(Path basePath, Path path) {
        Path absoluteBasePath = basePath.toAbsolutePath();
        Path normalizedAbsolutePath = absoluteBasePath.resolve(path).normalize();

        if (normalizedAbsolutePath.startsWith(absoluteBasePath)) {
            return Optional.of(absoluteBasePath.relativize(normalizedAbsolutePath));
        }

        return Optional.empty();
    }

    public static boolean isValidPath(Path basePath, Path path) {
        return normalize(basePath, path).isPresent();
    }

    public static List<Path> expandDirectories(Path basePath, List<Path> paths) throws IOException {
        ArrayList<Path> result = new ArrayList<>();
        for (Path path : paths) {
            result.addAll(expandDirectory(basePath, path));
        }

        return result;
    }

    public static List<Path> expandDirectory(Path basePath, Path path) throws IOException {
        return Files
                .walk(basePath.resolve(path).normalize())
                .filter(subPath -> !Files.isDirectory(subPath))
                .map(subPath -> normalize(basePath, subPath))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
