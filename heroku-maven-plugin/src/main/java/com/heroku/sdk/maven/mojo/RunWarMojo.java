package com.heroku.sdk.maven.mojo;

import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.lib.running.RunWebApp;
import com.heroku.sdk.maven.MavenLogOutputAdapter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Starts the web application in a way that is very similar to how it is run on Heroku. JAVA_OPTS and WEBAPP_RUNNER_OPTS
 * specified in configVars will also be picked up by this goal and used to run your application.
 */
@Mojo(name = "run-war", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.PACKAGE)
public class RunWarMojo extends AbstractHerokuMojo {

  @Override
  public void execute() throws MojoExecutionException {
    OutputAdapter outputAdapter = new MavenLogOutputAdapter(getLog(), logProgress);

    Path projectDirectory = super.mavenProject.getBasedir().toPath();

    List<String> javaOptions = splitOptions(configVars.getOrDefault("JAVA_OPTS", ""));
    List<String> webappRunnerOptions = splitOptions(configVars.getOrDefault("WEBAPP_RUNNER_OPTS", ""));

    Path warFilePath = null;
    try {
      warFilePath = findWarFilePath(projectDirectory).orElseThrow(() -> new MojoExecutionException("Could not find WAR file to run!"));
    } catch (IOException e) {
      throw new MojoExecutionException("Could not find WAR file to run!", e);
    }

    try {
      RunWebApp.run(warFilePath, javaOptions, webappRunnerOptions, webappRunnerVersion, outputAdapter);
    } catch (IOException | InterruptedException e) {
      throw new MojoExecutionException("Exception while running webapp-runner!", e);
    }
  }

  private List<String> splitOptions(String optionString) {
    return Arrays.stream(optionString.split(" "))
            .filter(string -> !string.trim().isEmpty())
            .collect(Collectors.toList());
  }
}
