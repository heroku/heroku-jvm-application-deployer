package com.heroku.maven.executor;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public class CopyDependencies extends MojoExecutor {

  public static void execute(MavenProject mavenProject,
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
}
