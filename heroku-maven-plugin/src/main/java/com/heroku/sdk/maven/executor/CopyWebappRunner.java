package com.heroku.sdk.maven.executor;

import com.heroku.sdk.deploy.DeployWar;
import com.heroku.sdk.deploy.utils.WebappRunnerResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

public class CopyWebappRunner extends MojoExecutor {

    public static void execute(MavenProject mavenProject,
                               MavenSession mavenSession,
                               BuildPluginManager pluginManager) throws MojoExecutionException {
        execute(mavenProject, mavenSession, pluginManager, DeployWar.DEFAULT_WEBAPP_RUNNER_VERSION);
    }

    public static void execute(MavenProject mavenProject,
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

