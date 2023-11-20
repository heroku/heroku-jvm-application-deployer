package com.heroku.sdk.deploy.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtils {

    public static <T> Properties loadOrEmptyFromClasspath(Class<T> clazz, String name) {
        final Properties properties = new Properties();
        try (final InputStream stream = clazz.getResourceAsStream(name)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            // Ignore exception, this will return empty properties later.
        }

        return properties;
    }

    /**
     * Loads the pom.properties for an artifact from classpath.
     *
     * @param clazz The class of which classloader should be used to read the pom.properties
     * @param groupId The groupId for which the pom.properties should be loaded
     * @param artifactId The artifactId for which the pom.properties should be loaded
     * @param <T> The type of the class modeled by the given Class object
     * @return The loaded properties
     *
     * @see <a href="http://maven.apache.org/shared/maven-archiver/#class_archive">http://maven.apache.org/shared/maven-archiver/#class_archive</a>
     */
    public static <T> Properties loadPomPropertiesOrEmptyFromClasspath(Class<T> clazz, String groupId, String artifactId) {
        return loadOrEmptyFromClasspath(clazz, String.format("/META-INF/maven/%s/%s/pom.properties", groupId, artifactId));
    }
}
