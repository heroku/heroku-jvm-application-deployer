package com.heroku.sdk.maven.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Mojo(name="eclipse-launch-config")
public class EclipseLaunchConfigMojo extends AbstractHerokuMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      if (mavenProject.getPackaging().equals("war")) {
        writeLaunchConfig("heroku-deploy-war.launch", "heroku:deploy-war");
        writeLaunchConfig("heroku-run-war.launch","heroku:run-war");
      } else {
        writeLaunchConfig("heroku-deploy.launch", "heroku:deploy");
      }

      writeLaunchConfig("heroku-dashboard.launch", "heroku:dashboard");
    } catch (IOException e) {
      throw new MojoFailureException("Could not create launch configuration files!", e);
    }
  }

  private void writeLaunchConfig(String filename, String goal) throws IOException {
    FileOutputStream outputStream = new FileOutputStream(filename);

    byte[] bytes = generateLaunchConfig(goal).getBytes(StandardCharsets.UTF_8);
    outputStream.write(bytes);
    outputStream.close();
  }

  private String generateLaunchConfig(String goal) {
    String[] lines = {
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>",
      "<launchConfiguration type=\"org.eclipse.m2e.Maven2LaunchConfigurationType\">",
      "<booleanAttribute key=\"M2_DEBUG_OUTPUT\" value=\"false\"/>",
      "<stringAttribute key=\"M2_GOALS\" value=\"" + goal + "\"/>",
      "<booleanAttribute key=\"M2_NON_RECURSIVE\" value=\"false\"/>",
      "<booleanAttribute key=\"M2_OFFLINE\" value=\"false\"/>",
      "<stringAttribute key=\"M2_PROFILES\" value=\"\"/>",
      "<listAttribute key=\"M2_PROPERTIES\"/>",
      "<stringAttribute key=\"M2_RUNTIME\" value=\"EMBEDDED\"/>",
      "<booleanAttribute key=\"M2_SKIP_TESTS\" value=\"false\"/>",
      "<intAttribute key=\"M2_THREADS\" value=\"1\"/>",
      "<booleanAttribute key=\"M2_UPDATE_SNAPSHOTS\" value=\"false\"/>",
      "<stringAttribute key=\"M2_USER_SETTINGS\" value=\"\"/>",
      "<booleanAttribute key=\"M2_WORKSPACE_RESOLUTION\" value=\"false\"/>",
      "<booleanAttribute key=\"org.eclipse.jdt.launching.ATTR_USE_START_ON_FIRST_THREAD\" value=\"true\"/>",
      "<stringAttribute key=\"org.eclipse.jdt.launching.WORKING_DIRECTORY\" value=\"" + mavenProject.getBasedir() + "\"/>",
      "</launchConfiguration>"
    };

    StringBuilder builder = new StringBuilder();
    for (String line : lines) {
      builder.append(line).append("\n");
    }

    return builder.toString();
  }
}
