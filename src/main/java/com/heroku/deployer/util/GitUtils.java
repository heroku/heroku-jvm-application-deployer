package com.heroku.deployer.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class GitUtils {
    public static Optional<String> getHeadCommitHash(Path projectDirectory) throws IOException {
        try (Git git = Git.open(projectDirectory.toFile())) {
            return Optional.ofNullable(git.getRepository().resolve(Constants.HEAD))
                    .map(AnyObjectId::getName);

        } catch (RepositoryNotFoundException e) {
            return Optional.empty();
        }
    }
}
