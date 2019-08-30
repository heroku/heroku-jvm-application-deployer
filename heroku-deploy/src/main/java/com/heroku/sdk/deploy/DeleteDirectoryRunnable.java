// $Id\$
package com.heroku.sdk.deploy;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

// from https://stackoverflow.com/questions/15022219/does-files-createtempdirectory-remove-the-directory-after-jvm-exits-normally
public class DeleteDirectoryRunnable implements Runnable {

    private final Path path;

    private static final SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file,
                                         @SuppressWarnings("unused") BasicFileAttributes attrs)
                throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e)
                throws IOException {
            if (e == null) {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
            // directory iteration failed
            throw e;
        }
    };

    public DeleteDirectoryRunnable(Path path) {
        this.path = path;
    }

    @Override
    public void run() {
        try {
            Files.walkFileTree(path, visitor);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete " + path, e);
        }
    }
}
