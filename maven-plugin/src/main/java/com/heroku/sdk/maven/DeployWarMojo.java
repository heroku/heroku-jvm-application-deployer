package com.heroku.sdk.maven;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Deploys a war file to Heroku
 *
 * @goal deploy-war
 * @execute phase="package"
 */
public class DeployWarMojo extends HerokuWarMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    super.execute();
    try {
      (prepareWarFile()).deploy(
          new ArrayList<>(getIncludes()), getConfigVars(), jdkVersion, new HashMap<>(), buildFilename
      );
    } catch (Exception e) {
      throw new MojoFailureException("Failed to deploy application", e);
    }
  }
}
