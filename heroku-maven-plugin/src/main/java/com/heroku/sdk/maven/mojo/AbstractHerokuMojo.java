package com.heroku.sdk.maven.mojo;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractHerokuMojo extends AbstractMojo {

  @Parameter(defaultValue="${session}", readonly=true)
  protected MavenSession mavenSession;

  @Parameter(defaultValue="${project}", readonly=true)
  protected MavenProject mavenProject;

  @Component
  protected BuildPluginManager pluginManager;

  /**
   * The name of the Heroku app.
   * <br/>
   * Command line -Dheroku.appName=...
   */
  @Parameter(name="appName", property="heroku.appName")
  protected String appName = null;

  /**
   * The major version of the JDK Heroku with run the app with.
   * <br/>
   * Command line -Dheroku.jdkVersion=...
   */
  @Parameter(name="jdkVersion", property="heroku.jdkVersion")
  protected String jdkVersion = null;

  /**
   * Configuration variables that will be set on the Heroku app.
   */
  @Parameter(name="configVars")
  protected Map<String, String> configVars = null;

  /**
   * A set of file patterns to include.
   */
  @Parameter(name="includes")
  protected List<String> includes = new ArrayList<>();

  /**
   * If the target directory should also be included. Defaults to true.
   */
  @Parameter(name="includeTarget", defaultValue = "true")
  protected boolean includeTarget = true;

  /**
   * If upload progress should be logged to debug.
   */
  @Parameter(name="logProgress", defaultValue = "false")
  protected boolean logProgress = false;

  /**
   * The buildpacks to run against the partial build.
   */
  @Parameter(name="buildpacks")
  protected List<String> buildpacks = new ArrayList<>();

  /**
   * The process types used to run on Heroku (similar to Procfile).
   */
  @Parameter(name="processTypes", required = true)
  protected Map<String, String> processTypes = null;


  void deploy(Map<String, String> processTypes) throws MojoFailureException {
    /*List<File> includedDirs = getIncludes();
    if(includeTarget) {
      includedDirs.add(getTargetDir());
    }*/

    /*try {
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
    }*/
  }

}
