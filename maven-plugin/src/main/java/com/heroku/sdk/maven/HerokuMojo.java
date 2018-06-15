package com.heroku.sdk.maven;

import java.io.File;
import java.util.*;

import com.heroku.sdk.maven.executor.ListDependencies;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

public abstract class HerokuMojo extends AbstractMojo {

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

  /**
   * The project currently being build.
   *
   * @parameter property="project"
   * @required
   * @readonly
   */
  protected MavenProject mavenProject;

  /**
   * @parameter property="project.build.directory"
   * @readonly
   */
  private File outputPath;

  /**
   * The name of the Heroku app.
   * <br/>
   * Command line -Dheroku.appName=...
   *
   * @parameter property="heroku.appName"
   */
  protected String appName = null;

  /**
   * The major version of the JDK Heroku with run the app with.
   * <br/>
   * Command line -Dheroku.jdkVersion=...
   *
   * @parameter property="heroku.jdkVersion"
   */
  protected String jdkVersion = null;

  /**
   * Configuration variables that will be set on the Heroku app.
   *
   * @parameter property="heroku.configVars"
   */
  protected Map<String,String> configVars = null;

  /**
   * A set of file patterns to include in the zip.
   * @parameter alias="includes"
   */
  protected String[] mIncludes = new String[0];

  /**
   * If the target directory should also be included. Defaults to true.
   * @parameter
   */
  protected boolean includeTarget = true;

  /**
   * A filename where the slug is stored at, inside the heroku-target directory
   *
   * @parameter property="heroku.buildFilename"
   */
  protected String buildFilename = "build.tgz";

  /**
   * If upload progress should be logged to debug.
   *
   * @parameter property="heroku.logProgress"
   */
  protected boolean logProgess = false;

  /**
   * The buildpacks to run against the partial build
   *
   * @parameter property="heroku.buildpacks"
   */
  protected String[] buildpacks = new String[]{};

  protected File getTargetDir() {
    return outputPath;
  }

  protected Map<String,String> getConfigVars() {
    return configVars;
  }

  protected List<File> getIncludes() {
    List<File> files = new ArrayList<File>(mIncludes.length);

    for (String s : mIncludes) {
      if (s.contains("*")) {
        String[] dirs = s.split(File.separator);
        String pattern = dirs[dirs.length-1];
        File basedir = new File(mavenProject.getBasedir(), s.replace(pattern, ""));
        Collection<File> listFiles = FileUtils.listFiles(basedir, new WildcardFileFilter(pattern), null);
        files.addAll(listFiles);
      } else {
        files.add(new File(s));
      }
    }

    return files;
  }

  public void setIncludes(String[] includes) {
    mIncludes = includes;
  }

  public boolean isIncludeTarget() {
	return includeTarget;
  }

  public void setIncludeTarget(boolean includeTarget) {
	this.includeTarget = includeTarget;
  }

  public String getSlugFilename() {
    return buildFilename;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    ListDependencies.execute(this.mavenProject, this.mavenSession, this.pluginManager);
  }

  void deploy(Map<String, String> processTypes) throws MojoFailureException {
    List<File> includedDirs = getIncludes();
    if(isIncludeTarget()) {
      includedDirs.add(getTargetDir());
    }

    try {
      (new MavenApp(
          appName,
          getTargetDir().getParentFile(),
          getTargetDir(),
          Arrays.asList(buildpacks),
          getLog(),
          logProgess)
      ).deploy(
          includedDirs,
          getConfigVars(),
          jdkVersion,
          processTypes,
          buildFilename
      );
    } catch (Exception e) {
      throw new MojoFailureException("Failed to deploy application", e);
    }
  }

}
