package com.heroku.sdk.maven;

import com.heroku.sdk.maven.executor.CopyDependencies;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

/**
 * Deploys a Slug to upload to Heroku. Creates the slug only if needed.
 *
 * @goal release-slug
 * @execute phase="package"
 * @requiresDependencyResolution
 */
public class ReleaseSlugMojo extends HerokuMojo {

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

    List<File> includedDirs = getIncludes();
    if(isIncludeTarget()) {
      includedDirs.add(getTargetDir());
    }

    try {
      (new MavenApp(appName, getTargetDir().getParentFile(), getTargetDir(), getLog(), logProgess))
          .releaseSlug(slugFilename, processTypes, configVars, stack);
    } catch (FileNotFoundException e) {
      throw new MojoFailureException("The slug file was not found. You must run heroku:create-slug first", e);
    } catch (Exception e) {
      throw new MojoFailureException("Failed to deploy Slug", e);
    }
  }
}
