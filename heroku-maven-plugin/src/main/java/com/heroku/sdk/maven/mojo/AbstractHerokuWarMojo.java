package com.heroku.sdk.maven.mojo;

import com.heroku.sdk.maven.MavenWarApp;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

public abstract class AbstractHerokuWarMojo extends AbstractHerokuMojo {

  /**
   * The path to the war file that will be deployed with the `deploy-war` target. It is not necessary to set this
   * parameter when packaging is set to 'war'.
   */
  @Parameter(name = "warFile")
  protected File warFile = null;

  /**
   * The version of webapp-runner to use.
   */
  @Parameter(name="webappRunnerVersion")
  protected String webappRunnerVersion = "x.x.x"; // TODO: Why? Isn't it resolved by maven itself?

  protected MavenWarApp prepareWarFile() throws MojoExecutionException, MojoFailureException {
    /*if (null == warFile) {
      if (!"war".equals(mavenProject.getPackaging())) {
        throw new MojoExecutionException("Your packaging must be set to 'war' or you must define the '<warFile>' config to use this goal!");
      } else {
        File[] files = getTargetDir().listFiles((dir, name) -> name.endsWith(".war"));
        if (files.length == 0) {
          throw new MojoFailureException("Could not find WAR file! Must specify file path in plugin configuration.");
        } else {
          warFile = files[0];
        }
      }
    }

    if (processTypes != null && !processTypes.isEmpty()) {
      getLog().warn("The <processTypes> value will be ignored when deploying a WAR file. Use `heroku:deploy` goal for custom processes.");
    }

    CopyWebappRunner.execute(this.mavenProject, this.mavenSession, this.pluginManager, webappRunnerVersion);

    File webappRunnerJar = new File(getTargetDir(), "dependency/webapp-runner.jar");

    return new MavenWarApp(
        appName,
        warFile,
        webappRunnerJar,
        getTargetDir().getParentFile(),
        getTargetDir(),
        getLog(),
            logProgress,
        buildpacks);

     */
    return null;
  }
}
