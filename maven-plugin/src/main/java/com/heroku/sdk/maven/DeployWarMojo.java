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

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    CopyWebappRunner.execute(this.mavenProject, this.mavenSession, this.pluginManager);

    File webappRunnerJar = new File(getTargetDir(), "dependency/webapp-runner.jar");

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

    try {
      (new MavenWarApp(appName, warFile, webappRunnerJar, getTargetDir().getParentFile(), getTargetDir(), getLog())).deploy(
          new ArrayList<File>(getIncludes()), getConfigVars(), jdkVersion, jdkUrl
      );
    } catch (Exception e) {
      throw new MojoFailureException("Failed to deploy application", e);
    }
  }
}
