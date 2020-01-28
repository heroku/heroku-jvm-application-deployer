package com.heroku.sdk.maven.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name="dashboard")
public class DashboardMojo extends AbstractHerokuMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (appName == null) {
      throw new MojoFailureException("Heroku app name not configured.");
    }

    String url = "https://dashboard.heroku.com/apps/" + appName;
    try {
      java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
    } catch (java.io.IOException e) {
      throw new MojoFailureException("Failed to open dashboard: " + url, e);
    }
  }
}
