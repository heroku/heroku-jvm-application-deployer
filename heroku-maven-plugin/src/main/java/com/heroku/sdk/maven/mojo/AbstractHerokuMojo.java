package com.heroku.sdk.maven.mojo;

import com.heroku.sdk.deploy.Constants;
import com.heroku.sdk.deploy.util.PathUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public abstract class AbstractHerokuMojo extends AbstractMojo {

  @Parameter(defaultValue="${session}", readonly=true)
  protected MavenSession mavenSession;

  @Parameter(defaultValue="${project}", readonly=true)
  protected MavenProject mavenProject;

  @Component
  protected BuildPluginManager pluginManager;

  /**
   * The name of the Heroku app.
   */
  @Parameter(name="appName", property="heroku.appName")
  protected String appName = null;

  /**
   * The build version of the application to deploy.
   */
  @Parameter(name="buildVersion", property="heroku.buildVersion", defaultValue = "${project.version}")
  protected String buildVersion = null;

  /**
   * The version of the JDK Heroku with run the app with.
   */
  @Parameter(name="jdkVersion", property="heroku.jdkVersion")
  protected String jdkVersion = null;

  /**
   * Configuration variables that will be set on the Heroku app.
   */
  @Parameter(name="configVars")
  protected Map<String, String> configVars = Collections.emptyMap();

  /**
   * A set of file patterns to include.
   */
  @Parameter(name="includes")
  protected List<String> includes = new ArrayList<>();

  /**
   * If the target directory should also be included. Defaults to true.
   */
  @Parameter(name="includeTarget", defaultValue = "true")
  protected boolean includeTarget = true;

  /**
   * If upload progress should be logged to debug.
   */
  @Parameter(name="logProgress", defaultValue = "false")
  protected boolean logProgress = false;

  /**
   * The buildpacks to run against the partial build.
   */
  @Parameter(name="buildpacks")
  protected List<String> buildpacks = new ArrayList<>();

  /**
   * The process types used to run on Heroku (similar to Procfile).
   */
  @Parameter(name="processTypes")
  protected Map<String, String> processTypes = Collections.emptyMap();

  /**
   * The path to the war file that will be deployed with the `deploy-war` target.
   */
  @Parameter(name="warFile")
  protected String warFile = null;

  /**
   * The version of webapp-runner to use.
   */
  @Parameter(name="webappRunnerVersion")
  protected String webappRunnerVersion = Constants.DEFAULT_WEBAPP_RUNNER_VERSION;

  /**
   * Common helper to find the projects WAR file either by referencing the explicitly configured WAR file or searching
   * the build directory for a WAR file. This implementation assumes it is run as part of a goal and will throw
   * MojoExecutionExceptions with user-facing error messages.
   *
   * @param projectDirectory The root directory of the project
   * @return An optional Path to the WAR file if one could be found
   * @throws MojoExecutionException If there is no explicitly configured WAR file path or the projects packaged as a WAR
   * @throws IOException If an IO error occurred during WAR file search
   */
  protected Optional<Path> findWarFilePath(Path projectDirectory) throws MojoExecutionException, IOException {
    if (warFile == null) {
      if (!mavenProject.getPackaging().equals("war")) {
        throw new MojoExecutionException("Your packaging must be set to 'war' or you must define the '<warFile>' config to use this goal!");
      }

      return Files
              .find(Paths.get(mavenProject.getBuild().getDirectory()), Integer.MAX_VALUE, (path, attributes) -> path.toString().endsWith(".war"))
              .findFirst()
              .flatMap(path -> PathUtils.normalize(projectDirectory, path));
    } else {
      return PathUtils
              .normalize(projectDirectory, Paths.get(warFile));
    }
  }
}
