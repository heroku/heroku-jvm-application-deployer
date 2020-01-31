package com.heroku.sdk.deploy.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class GitUtils {
    public static Optional<String> getHeadCommitHash(Path projectDirectory) throws IOException {
        try {
            Git git = Git.open(projectDirectory.toFile());
            ObjectId objectId = git.getRepository().resolve(Constants.HEAD);
            return Optional.of(objectId.getName());

        } catch (RepositoryNotFoundException e) {
            return Optional.empty();
        }
    }
}
