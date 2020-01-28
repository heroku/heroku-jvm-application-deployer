package com.heroku.sdk.maven;

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Deploys an application to Heroku without resolving dependencies, packaging, or vendoring dependencies. This
 * goal can be used to execute standalone, or be bound to a different phase in the lifecycle.
 */
@Mojo(name="deploy-only")
public class DeployOnlyMojo extends AbstractHerokuMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    System.out.println("EXECUTE ORDER 66");
  }
}
