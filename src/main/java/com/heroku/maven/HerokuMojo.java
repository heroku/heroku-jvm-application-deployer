package com.heroku.maven;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.URL;
import java.util.Map;

/**
 * Deploys an application to Heroku
 *
 * @goal heroku
 * @requiresDependencyResolution test
 */
public class HerokuMojo extends AbstractMojo {
  /**
   * The maven project.
   *
   * @parameter expression="project"
   * @readonly
   */
  private MavenProject project;

  /**
   * @parameter expression="project.build.directory/project.build.finalName"
   * @readonly
   */
  private File outputPath;

  /**
   * The name of the Heroku app.
   * <br/>
   * Command line -Dheroku.appName=...
   *
   * @required
   * @parameter expression="heroku.appName"
   */
  protected String appName = null;

  /**
   * The major version of the JDK Heroku with run the app with.
   * <br/>
   * Command line -Dheroku.jdkVersion=...
   *
   * @parameter expression="heroku.jdkVersion"
   *            default-value="1.7"
   */
  protected String jdkVersion = null;

  /**
   * The URL of the JDK binaries Heroku will use.
   * <br/>
   * Command line -Dheroku.jdkUrl=...
   *
   * @parameter expression="heroku.jdkUrl"
   */
  protected String jdkUrl = null;

  /**
   * Configuration variables that will be set on the Heroku app.
   *
   * @parameter expression="heroku.configVars"
   */
  protected Map<String,String> configVars = null;

  /**
   * The process types used to run on Heroku (similar to Procfile).
   *
   * @required
   * @parameter expression="heroku.configVars"
   */
  protected Map<String,String> processTypes = null;

  private String encodedApiKey = null;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

  }

  private void deploy(File targetDir) {

    getLog().info("---> Packaging application...");
    getLog().info("     - app: " + appName);

    // build the slug

    // log included files

    // copy included files to getAppDir()

    // add slug extras

    // createSlugData

    // create Tar

    // config var stuff...

    // create slug

    // upload slug

    // release slug
  }

  private File getHerokuDir() {
    return new File(getTargetDir(), "heroku");
  }

  private File getAppDir() {
    return new File(getHerokuDir(), "app");
  }

  private File getAppTargetDir() {
    return new File(getAppDir(), "target");
  }

  private File getTargetDir() {
//    Model model = project.getModel();
//    Build build = model.getBuild();
//    return new File(build.getDirectory());
    return outputPath;
  }

  private String getEncodedApiKey() throws IOException {
    if (encodedApiKey == null) {
      String apiKey = System.getenv("HEROKU_API_KEY");
      if (null == apiKey || apiKey.equals("")) {
        ProcessBuilder pb = new ProcessBuilder().command("heroku", "auth:token");
        Process p = pb.start();

        BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        apiKey = "";
        while ((line = bri.readLine()) != null) {
          apiKey += line;
        }
      }
      encodedApiKey = new BASE64Encoder().encode((":" + apiKey).getBytes());
    }
    return encodedApiKey;
  }

  private String buildSlug(File targetDir, File appTargetDir, File herokuDir) {

    return "";
  }
}
