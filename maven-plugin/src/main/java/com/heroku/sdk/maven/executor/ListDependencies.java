package com.heroku.sdk.maven.executor;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;

public class ListDependencies extends MojoExecutor {

    public static final String FILENAME = "mvn-dependency-list.log";

    public static void execute(MavenProject mavenProject,
                               MavenSession mavenSession,
                               BuildPluginManager pluginManager) throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version("2.4")
                ),
                goal("list"),
                configuration(
                        element(name("outputFile"), "${project.build.directory}/" + FILENAME)
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }
}
