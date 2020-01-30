package com.heroku.sdk.maven.mojo;

import com.heroku.sdk.maven.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Deploys an application to Heroku.
 */
@Mojo(name="deploy", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class DeployMojo extends AbstractHerokuDeployMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    /* We vendor all dependencies into the build directory before deploying to ensure those are copied to the source
     * blob as well. This avoids that users have to explicitly copy dependencies to /target and is helpful in cases
     * where the user does not have deep knowledge about the Maven build process and/or did not configure many of the
     * plugins parameters.
     *
     * Advanced users should use DeployOnlyMojo which does not copy the dependencies.
     */
    MojoExecutor.copyDependenciesToBuildDirectory(super.mavenProject, super.mavenSession, super.pluginManager);
    deploy(Mode.GENERIC);
  }
}
