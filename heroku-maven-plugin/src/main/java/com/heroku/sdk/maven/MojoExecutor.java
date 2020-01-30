package com.heroku.sdk.maven;

import com.heroku.sdk.deploy.lib.resolver.WebappRunnerResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

public class MojoExecutor {
    public static void copyDependenciesToBuildDirectory(MavenProject mavenProject,
                                                        MavenSession mavenSession,
                                                        BuildPluginManager pluginManager) throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.4")
                ),
                goal("copy-dependencies"),
                configuration(
                        element(name("outputDirectory"), "${project.build.directory}/dependency")
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }

    public static Path createDependencyListFile(MavenProject mavenProject,
                                                MavenSession mavenSession,
                                                BuildPluginManager pluginManager) throws MojoExecutionException, IOException {

        Path path = Files.createTempFile("heroku-maven-plugin", "mvn-dependency-list.log");

        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.4")
                ),
                goal("list"),
                configuration(
                        element(name("outputFile"), path.toString())
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );

        return path;
    }

    public static void copyWebappRunner(MavenProject mavenProject,
                                        MavenSession mavenSession,
                                        BuildPluginManager pluginManager,
                                        String webappRunnerVersion) throws MojoExecutionException {

        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.4")
                ),
                goal("copy"),
                configuration(
                        element(name("artifactItems"),
                                element(name("artifactItem"),
                                        element(name("groupId"), WebappRunnerResolver.getGroupIdForVersion(webappRunnerVersion)),
                                        element(name("artifactId"), "webapp-runner"),
                                        element(name("version"), webappRunnerVersion),
                                        element(name("destFileName"), "webapp-runner.jar")))
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }
}
