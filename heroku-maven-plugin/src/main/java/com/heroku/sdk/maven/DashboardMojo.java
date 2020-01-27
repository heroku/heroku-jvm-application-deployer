package com.heroku.sdk.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Opens the Heroku Dashboard for an application
 *
 * @goal dashboard
 */
public class DashboardMojo extends HerokuMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    String url = "https://dashboard.heroku.com/apps/" + appName;
    try {
      java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
    } catch (java.io.IOException e) {
      throw new MojoFailureException("Failed to open dashboard: " + url, e);
    }
  }
}
