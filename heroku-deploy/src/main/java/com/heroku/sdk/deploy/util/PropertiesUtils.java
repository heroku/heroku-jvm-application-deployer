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
     * @link http://maven.apache.org/shared/maven-archiver/#class_archive
     */
    public static <T> Properties loadPomPropertiesOrEmptyFromClasspath(Class<T> clazz, String groupId, String artifactId) {
        return loadOrEmptyFromClasspath(clazz, String.format("/META-INF/maven/%s/%s/pom.properties", groupId, artifactId));
    }
}
