package com.heroku.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Deploys an application to Heroku
 *
 * @goal deploy
 * @execute phase="package"
 */
public class DeployMojo extends HerokuMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getTargetDir().mkdir();

    List<File> includedDirs = new ArrayList<File>();
    includedDirs.add(getTargetDir());

    try {
      (new MavenApp(appName, getTargetDir().getParentFile(), getTargetDir(), getLog())).deploy(
          includedDirs, getConfigVars(), jdkVersion, jdkUrl, getProcessTypes()
      );
    } catch (Exception e) {
      throw new MojoFailureException("Failed to deploy application", e);
    }
  }
}
