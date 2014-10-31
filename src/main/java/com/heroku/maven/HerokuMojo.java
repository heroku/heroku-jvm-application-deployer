package com.heroku.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
   * The name of the Heroku app.
   * <br/>
   * Command line -Dheroku.appName=...
   *
   * @required
   * @parameter expression="${heroku.appName}"
   */
  protected String appName = null;

  /**
   * The major version of the JDK Heroku with run the app with.
   * <br/>
   * Command line -Dheroku.jdkVersion=...
   *
   * @parameter expression="${heroku.jdkVersion}"
   *            default-value="1.7"
   */
  protected String jdkVersion = null;

  /**
   * The URL of the JDK binaries Heroku will use.
   * <br/>
   * Command line -Dheroku.jdkUrl=...
   *
   * @parameter expression="${heroku.jdkUrl}"
   */
  protected String jdkUrl = null;

  /**
   * Configuration variables that will be set on the Heroku app.
   *
   * @parameter expression="${heroku.configVars}"
   */
  protected Map<String,String> configVars = null;

  /**
   * The process types used to run on Heroku (similar to Procfile).
   *
   * @required
   * @parameter expression="${heroku.configVars}"
   */
  protected Map<String,String> processTypes = null;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

  }

  private String getApiKey() throws IOException {
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
    return new BASE64Encoder().encode((":" + apiKey).getBytes());
  }

  private String buildSlug(File targetDir, File appTargetDir, File herokuDir) {

    return "";
  }
}
