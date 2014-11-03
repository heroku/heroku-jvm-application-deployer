package com.heroku.maven;

import com.heroku.maven.executor.CopyDependencies;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deploys an application to Heroku
 *
 * @goal deploy
 * @execute phase="package"
 * @requiresDependencyResolution
 */
public class DeployMojo extends HerokuMojo {

  /**
   * The process types used to run on Heroku (similar to Procfile).
   *
   * @required
   * @parameter property="heroku.processTypes"
   */
  protected Map<String,String> processTypes = null;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    CopyDependencies.execute(this.mavenProject, this.mavenSession, this.pluginManager);

    List<File> includedDirs = new ArrayList<File>();
    includedDirs.add(getTargetDir());

    try {
      (new MavenApp(appName, getTargetDir().getParentFile(), getTargetDir(), getLog())).deploy(
          includedDirs, getConfigVars(), jdkVersion, jdkUrl, processTypes
      );
    } catch (Exception e) {
      throw new MojoFailureException("Failed to deploy application", e);
    }
  }
}
