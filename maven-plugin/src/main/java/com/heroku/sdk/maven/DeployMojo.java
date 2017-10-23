package com.heroku.sdk.maven;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.heroku.sdk.maven.executor.CopyDependencies;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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
    super.execute();
    CopyDependencies.execute(this.mavenProject, this.mavenSession, this.pluginManager);

    List<File> includedDirs = getIncludes();
    if(isIncludeTarget()) {
      includedDirs.add(getTargetDir());
    }

    try {
      (new MavenApp(appName, getTargetDir().getParentFile(), getTargetDir(), Arrays.asList(buildpacks), getLog(), logProgess)).deploy(
              includedDirs, getConfigVars(), jdkUrl == null ? jdkVersion : jdkUrl, stack, processTypes, slugFilename
      );
    } catch (Exception e) {
      throw new MojoFailureException("Failed to deploy application", e);
    }
  }
}
