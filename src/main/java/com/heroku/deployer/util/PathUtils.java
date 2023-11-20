package com.heroku.deployer.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        Path absoluteBasePath = basePath.toAbsolutePath().normalize();
        Path normalizedAbsolutePath = absoluteBasePath.resolve(path).normalize();

        // Resolve to actual paths on disk to ensure normalization works correctly when symlinks are in play.
        // This issue was uncovered via Rust integration tests where the temporary path for the app fixture was
        // placed in a path that contained a symlink. See: https://github.com/rust-lang/rust/issues/99608
        // Do not remove this when the Rust issue has been fixed - it's just an example failure.
        try {
            if (Files.exists(absoluteBasePath)) {
                absoluteBasePath = absoluteBasePath.toRealPath();
            }

            if (Files.exists(normalizedAbsolutePath)) {
                normalizedAbsolutePath = normalizedAbsolutePath.toRealPath();
            }
        } catch (IOException e) {
            return Optional.empty();
        }

        if (normalizedAbsolutePath.startsWith(absoluteBasePath)) {
            return Optional.of(absoluteBasePath.relativize(normalizedAbsolutePath));
        }

        return Optional.empty();
    }

    public static Optional<String> getFileExtension(Path path) {
        return Optional.ofNullable(path.getFileName())
                .map(Path::toString).filter(fileName -> fileName.contains("."))
                .map(fileName -> fileName.substring(fileName.lastIndexOf('.') + 1));
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

    public static String separatorsToUnix(Path path) {
        // Path will normalize separators back to Windows when run on Windows. We have to fall back to a String here.
        return path.toString().replace('\\', '/');
    }
}
