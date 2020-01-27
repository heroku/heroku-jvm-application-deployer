package com.heroku.sdk.maven;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.heroku.sdk.maven.executor.CopyDependencies;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Deploys an application to Heroku without resolving dependencies, packaging, or vendoring dependencies. This
 * goal can be used to execute standalone, or be bound to a different phase in the lifecycle.
 *
 * @goal deploy-only
 */
public class DeployOnlyMojo extends HerokuMojo {

  /**
   * The process types used to run on Heroku (similar to Procfile).
   *
   * @required
   * @parameter property="heroku.processTypes"
   */
  protected Map<String, String> processTypes = null;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    super.execute();
    deploy(processTypes);
  }
}