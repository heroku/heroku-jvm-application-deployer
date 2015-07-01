package com.heroku.sdk.maven;

import com.heroku.sdk.maven.executor.CopyWebappRunner;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Map;

public abstract class HerokuWarMojo extends HerokuMojo {

  /**
   * The path to the war file that will be deployed with the `deploy-war` target.
   *
   * @parameter property="heroku.warFile"
   */
  protected File warFile = null;

  /**
   * The process types used to run on Heroku (similar to Procfile).
   *
   * @parameter property="heroku.processTypes"
   */
  protected Map<String,String> processTypes = null;

  /**
   * The version of webapp-runner to use.
   *
   * @parameter property="heroku.webappRunnerVersion"
   */
  protected String webappRunnerVersion = CopyWebappRunner.DEFAULT_WEBAPP_RUNNER_VERSION;

  protected MavenWarApp prepareWarFile() throws MojoExecutionException, MojoFailureException {
    if (null == warFile) {
      if (!"war".equals(mavenProject.getPackaging())) {
        throw new MojoExecutionException("Your packaging must be set to 'war' or you must define the '<warFile>' config to use this goal!");
      } else {
        File[] files = getTargetDir().listFiles(new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return name.endsWith(".war");
          }
        });
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

    return new MavenWarApp(appName, warFile, webappRunnerJar, getTargetDir().getParentFile(), getTargetDir(), getLog());
  }
}
