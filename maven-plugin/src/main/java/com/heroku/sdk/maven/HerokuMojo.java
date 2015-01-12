package com.heroku.sdk.maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
   * The Heorku runtime stack.
   * <br/>
   * Command line -Dheroku.stack=...
   *
   * @parameter property="heroku.stack"
   */
  protected String stack = "cedar-14";

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
   * A filename where the slug is stored at, inside the heroku-target directory
   * @parameter property="heroku.slugFileName"
   */
  protected String slugFileName;

  protected File getTargetDir() {
    return outputPath;
  }

  protected Map<String,String> getConfigVars() {
    return configVars;
  }

  protected List<File> getIncludes() {
    List<File> files = new ArrayList<File>(mIncludes.length);

    for (String s : mIncludes) {
      files.add(new File(s));
    }

    return files;
  }

  public void setIncludes(String[] includes) {
    mIncludes = includes;
  }

  public String getSlugFileName() {
    return slugFileName;
  }
}
