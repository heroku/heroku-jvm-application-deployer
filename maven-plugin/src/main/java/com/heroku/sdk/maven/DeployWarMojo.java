package com.heroku.sdk.maven;

import com.heroku.sdk.maven.executor.CopyWebappRunner;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

/**
 * Deploys a war file to Heroku
 *
 * @goal deploy-war
 * @execute phase="package"
 */
public class DeployWarMojo extends HerokuMojo {

  /**
   * The path to the war file that will be deployed with the `deploy-war` target.
   *
   * @parameter property="heroku.warFile"
   */
  protected File warFile = null;

  /**
   * The version of webapp-runner to use.
   *
   * @parameter property="heroku.webappRunnerVersion"
   */
  protected String webappRunnerVersion = CopyWebappRunner.DEFAULT_WEBAPP_RUNNER_VERSION;

  protected void prepareWarFile() throws MojoExecutionException, MojoFailureException {


    CopyWebappRunner.execute(this.mavenProject, this.mavenSession, this.pluginManager, webappRunnerVersion);

    if (null == warFile) {
      File [] files = getTargetDir().listFiles(new FilenameFilter() {
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

  protected MavenWarApp createWarFile() throws MojoExecutionException, MojoFailureException {
    prepareWarFile();

    File webappRunnerJar = new File(getTargetDir(), "dependency/webapp-runner.jar");

    return new MavenWarApp(appName, warFile, webappRunnerJar, getTargetDir().getParentFile(), getTargetDir(), getLog());
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      (createWarFile()).deploy(
          new ArrayList<File>(getIncludes()), getConfigVars(), jdkUrl == null ? jdkVersion : jdkUrl, stack
      );
    } catch (Exception e) {
      throw new MojoFailureException("Failed to deploy application", e);
    }
  }
}
