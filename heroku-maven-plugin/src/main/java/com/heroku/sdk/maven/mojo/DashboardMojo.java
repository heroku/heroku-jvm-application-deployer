package com.heroku.sdk.maven.mojo;

import com.heroku.sdk.deploy.lib.resolver.AppNameResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.net.URI;
import java.util.Optional;

@Mojo(name="dashboard")
public class DashboardMojo extends AbstractHerokuMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      AppNameResolver.resolve(super.mavenProject.getBasedir().toPath(), () -> Optional.ofNullable(super.appName));
      String url = "https://dashboard.heroku.com/apps/" + appName;
      java.awt.Desktop.getDesktop().browse(URI.create(url));
    } catch (java.io.IOException e) {
      throw new MojoFailureException("Could not open dashboard!", e);
    }
  }
}
