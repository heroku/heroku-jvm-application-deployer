package com.heroku.sdk.deploy.lib.resolver;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppNameResolver {

    /**
     * Resolves the Heroku app name based on the given project directory.
     * This uses the already established resolution order from 2.x to ensure backwards compatibility:
     *
     * 1. The heroku.properties file
     * 2. The heroku.appName system property
     * 3. Custom resolution
     * 4. Git remote pointing to an Heroku app
     *
     * @param projectDirectory The projects root directory.
     * @param customResolver A custom resolver for the app name. Most likely a setting from a build tool like Maven or sbt.
     * @return If possible, the resolved app name.
     * @throws IOException Resolving requires IO operations which might fail.
     */
    public static Optional<String> resolve(Path projectDirectory, Supplier<Optional<String>> customResolver) throws IOException {
        Optional<String> herokuPropertiesAppName = resolveViaHerokuPropertiesFile(projectDirectory);
        if (herokuPropertiesAppName.isPresent()) {
            return herokuPropertiesAppName;
        }

        Optional<String> systemPropertiesAppName = resolveViaSystemProperty();
        if (systemPropertiesAppName.isPresent()) {
            return systemPropertiesAppName;
        }

        Optional<String> customResolverAppName = customResolver.get();
        if (customResolverAppName.isPresent()) {
            return customResolverAppName;
        }

        return resolveViaHerokuGitRemote(projectDirectory);
    }

    private static Optional<String> resolveViaHerokuGitRemote(Path rootDirectory) throws IOException {
        try {
            Git gitRepo = Git.open(rootDirectory.toFile());
            Config config = gitRepo.getRepository().getConfig();

            for (String remoteName : config.getSubsections("remote")) {
                String remoteUrl = config.getString("remote", remoteName, "url");

                for (Pattern gitRemoteUrlAppNamePattern : GIT_REMOTE_URL_APP_NAME_PATTERNS) {
                    Matcher matcher = gitRemoteUrlAppNamePattern.matcher(remoteUrl);
                    if (matcher.matches()) {
                        return Optional.of(matcher.group(1));
                    }
                }
            }
        } catch (RepositoryNotFoundException e) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    private static Optional<String> resolveViaHerokuPropertiesFile(Path rootDirectory) throws IOException {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(rootDirectory.resolve("heroku.properties").toFile()));
        } catch (FileNotFoundException e) {
            return Optional.empty();
        }

        return Optional.ofNullable(properties.getProperty("heroku.appName"));
    }

    private static Optional<String> resolveViaSystemProperty() {
        return Optional.ofNullable(System.getProperty("heroku.appName"));
    }

    private static final List<Pattern> GIT_REMOTE_URL_APP_NAME_PATTERNS;

    static {
        ArrayList<Pattern> patterns = new ArrayList<>();
        patterns.add(Pattern.compile("https://git\\.heroku\\.com/(.*?)\\.git"));
        patterns.add(Pattern.compile("git@heroku\\.com:(.*?)\\.git"));
        GIT_REMOTE_URL_APP_NAME_PATTERNS = Collections.unmodifiableList(patterns);
    }
}
