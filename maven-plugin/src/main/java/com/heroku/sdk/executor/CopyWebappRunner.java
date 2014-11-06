package com.heroku.sdk.executor;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public class CopyWebappRunner extends MojoExecutor {

  public static void execute(MavenProject mavenProject,
                             MavenSession mavenSession,
                             BuildPluginManager pluginManager) throws MojoExecutionException {
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
                    element(name("groupId"), "com.github.jsimone"),
                    element(name("artifactId"), "webapp-runner"),
                    element(name("version"), "7.0.40.0"),
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

