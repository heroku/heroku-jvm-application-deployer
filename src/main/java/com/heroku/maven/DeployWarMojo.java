package com.heroku.maven;

import com.heroku.api.WarApp;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    getTargetDir().mkdir();

    List<File> includedDirs = new ArrayList<File>();
    includedDirs.add(getTargetDir());

    try {
      (new WarApp(appName, warFile, getTargetDir().getParentFile(), getTargetDir())).deploy(
          includedDirs, getConfigVars(), jdkVersion, jdkUrl
      );
    } catch (Exception e) {
      throw new MojoFailureException("Failed to deploy application", e);
    }
  }
}
