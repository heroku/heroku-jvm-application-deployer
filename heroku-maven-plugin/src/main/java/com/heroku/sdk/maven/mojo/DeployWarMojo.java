package com.heroku.sdk.maven.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Deploys a war file to Heroku.
 */
@Mojo(name="deploy-war", defaultPhase = LifecyclePhase.PACKAGE)
public class DeployWarMojo extends AbstractHerokuDeployMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    deploy(Mode.WAR);
  }
}
