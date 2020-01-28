package com.heroku.sdk.maven.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.*;

/**
 * Opens the Heroku Dashboard for an application
 *
 * @goal eclipse-launch-config
 */
public class EclipseLaunchConfigMojo extends AbstractMojo {

  /**
   * The project currently being build.
   *
   * @parameter property="project"
   * @required
   * @readonly
   */
  protected MavenProject mavenProject;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      if ("war".equals(mavenProject.getPackaging())) {
        copyFile("heroku-deploy-war.launch", "heroku:deploy-war");
        copyFile("heroku-run-war.launch", "heroku:run-war");
      } else {
        copyFile("heroku-deploy.launch", "heroku:deploy");
      }
      copyFile("heroku-dashboard.launch", "heroku:dashboard");
    } catch (IOException e) {
      throw new MojoFailureException("Could not create launch configuration files!", e);
    }
  }

  private void copyFile(String filename, String mavenGoal) throws IOException {
    getLog().info("Generating " + filename + " configuration...");
    BufferedWriter out = null;
    try {
      InputStream is = getClass().getResourceAsStream( "/heroku-eclipse-base-config.launch");
      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      FileWriter fw = new FileWriter(filename);
      out = new BufferedWriter(fw);

      String line;
      while ((line = br.readLine()) != null) {
        line = line.replace("%_MAVEN_GOAL_%", mavenGoal);
        line = line.replace("%_PROJECT_DIR_%", System.getProperty("user.dir"));
        out.write(line);
        out.write("\n");
      }
    } finally {
      if (null != out) out.close();
    }
  }
}
