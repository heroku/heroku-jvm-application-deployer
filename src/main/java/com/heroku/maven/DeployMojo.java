package com.heroku.maven;

import com.heroku.maven.executor.CopyDependencies;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
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
 * @requiresDependencyResolution
 */
public class DeployMojo extends HerokuMojo {

  /**
   * The current Maven session.
   *
   * @parameter property="session"
   * @required
   * @readonly
   */
  protected MavenSession mavenSession;

  /**
   * The Maven BuildPluginManager component.
   *
   * @component
   * @required
   */
  protected BuildPluginManager pluginManager;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getTargetDir().mkdir();

    CopyDependencies.execute(this.mavenProject, this.mavenSession, this.pluginManager);

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
