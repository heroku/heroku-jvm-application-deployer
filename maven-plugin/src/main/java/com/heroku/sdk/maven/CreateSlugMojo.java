package com.heroku.sdk.maven;

import com.heroku.sdk.maven.executor.CopyDependencies;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.List;

/**
 * Creates a Slug to upload to Heroku
 *
 * @goal create-slug
 * @execute phase="package"
 * @requiresDependencyResolution
 */
public class CreateSlugMojo extends HerokuMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    super.execute();
    CopyDependencies.execute(this.mavenProject, this.mavenSession, this.pluginManager);

    List<File> includedDirs = getIncludes();
    if(isIncludeTarget()) {
      includedDirs.add(getTargetDir());
    }

    try {
      (new MavenApp(appName, getTargetDir().getParentFile(), getTargetDir(), getLog()))
          .createSlug(slugFilename, includedDirs, jdkUrl == null ? jdkVersion : jdkUrl, stack);
    } catch (Exception e) {
      throw new MojoFailureException("Failed to create Slug", e);
    }
  }
}
