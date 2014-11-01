package com.heroku.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deploys an application to Heroku
 *
 * @goal deploy
 * @execute phase="package"
 */
public class HerokuMojo extends AbstractMojo {
  /**
   * The maven project.
   *
   * @parameter property="project"
   * @readonly
   */
  private MavenProject project;

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
   * @required
   * @parameter property="heroku.appName"
   */
  protected String appName = null;

  /**
   * The major version of the JDK Heroku with run the app with.
   * <br/>
   * Command line -Dheroku.jdkVersion=...
   *
   * @parameter property="heroku.jdkVersion"
   *            default-value="1.7"
   */
  protected String jdkVersion = null;

  /**
   * The URL of the JDK binaries Heroku will use.
   * <br/>
   * Command line -Dheroku.jdkUrl=...
   *
   * @parameter property="heroku.jdkUrl"
   */
  protected String jdkUrl = null;

  /**
   * Configuration variables that will be set on the Heroku app.
   *
   * @parameter property="heroku.configVars"
   */
  protected Map<String,String> configVars = null;

  /**
   * The process types used to run on Heroku (similar to Procfile).
   *
   * @required
   * @parameter property="heroku.processTypes"
   */
  protected Map<String,String> processTypes = null;

  private String encodedApiKey = null;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getTargetDir().mkdir();

    List<File> includedDirs = new ArrayList<File>();
    includedDirs.add(getTargetDir());

    try {
      (new MavenApp(appName, getTargetDir(), getLog())).deploy(
          includedDirs, getConfigVars(), jdkVersion, jdkUrl, getProcessTypes()
      );
    } catch (Exception e) {
      throw new MojoFailureException("Failed to deploy application", e);
    }
  }

  private File getTargetDir() {
    return outputPath;
  }

  private Map<String,String> getProcessTypes() {
    if (processTypes.isEmpty()) throw new IllegalArgumentException("Must provide a process type!");
    return processTypes;
  }

  private Map<String,String> getConfigVars() {

    return configVars;
  }
}
