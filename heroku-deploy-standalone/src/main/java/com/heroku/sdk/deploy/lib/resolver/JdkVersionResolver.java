package com.heroku.sdk.deploy.lib.resolver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

public class JdkVersionResolver {

    public static Optional<String> resolve(Path projectDirectory, Supplier<Optional<String>> customResolver) throws IOException {
        Optional<String> customResolverJdkVersion = customResolver.get();
        if (customResolverJdkVersion.isPresent()) {
            return customResolverJdkVersion;
        }

        Optional<String> systemPropertiesAppName = resolveViaSystemProperty();
        if (systemPropertiesAppName.isPresent()) {
            return systemPropertiesAppName;
        }

        return resolveViaSystemPropertiesFile(projectDirectory);
    }

    private static Optional<String> resolveViaSystemProperty() {
        return Optional.ofNullable(System.getProperty("heroku.jdkVersion"));
    }

    private static Optional<String> resolveViaSystemPropertiesFile(Path projectDirectory) throws IOException {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(projectDirectory.resolve("system.properties").toFile()));
        } catch (FileNotFoundException e) {
            return Optional.empty();
        }

        return Optional.ofNullable(properties.getProperty("java.runtime.version"));
    }
}
