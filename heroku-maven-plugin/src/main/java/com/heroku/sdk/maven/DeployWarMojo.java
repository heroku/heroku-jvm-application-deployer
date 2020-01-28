package com.heroku.sdk.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Deploys a war file to Heroku.
 */
@Mojo(name="deploy-war", defaultPhase = LifecyclePhase.PACKAGE)
public class DeployWarMojo extends AbstractHerokuWarMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
  }

  /*
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    super.execute();
    try {
      (prepareWarFile()).deploy(
          new ArrayList<>(getIncludes()), getConfigVars(), jdkVersion, new HashMap<String,String>(), buildFilename
      );
    } catch (Exception e) {
      throw new MojoFailureException("Failed to deploy application", e);
    }
  }*/
}
