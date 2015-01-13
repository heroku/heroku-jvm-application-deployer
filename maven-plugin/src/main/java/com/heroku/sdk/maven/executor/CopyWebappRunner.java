package com.heroku.sdk.maven.executor;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public class CopyWebappRunner extends MojoExecutor {

  public static final String DEFAULT_WEBAPP_RUNNER_VERSION = "7.0.57.1";

  public static void execute(MavenProject mavenProject,
                             MavenSession mavenSession,
                             BuildPluginManager pluginManager) throws MojoExecutionException {
    execute(mavenProject,mavenSession,pluginManager, DEFAULT_WEBAPP_RUNNER_VERSION);
  }

  public static void execute(MavenProject mavenProject,
                             MavenSession mavenSession,
                             BuildPluginManager pluginManager,
                             String webappRunnerversion) throws MojoExecutionException {
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
                    element(name("version"), webappRunnerversion),
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

