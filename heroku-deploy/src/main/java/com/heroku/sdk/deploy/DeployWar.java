package com.heroku.sdk.deploy;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DeployWar extends WarApp {

  private static final String WEBAPP_RUNNER_URL="http://central.maven.org/maven2/com/github/jsimone/webapp-runner/7.0.57.2/webapp-runner-7.0.57.2.jar";

  public DeployWar(String name, File warFile, URL webappRunnerUrl) throws IOException {
    super(name);
    this.warFile = warFile;

    this.webappRunnerJar = new File(getAppDir(), "webapp-runner.jar");
    FileUtils.copyURLToFile(webappRunnerUrl, webappRunnerJar);
  }

  @Override
  protected Map<String,String> defaultProcTypes() {
    Map<String,String> processTypes = new HashMap<String, String>();
    processTypes.put("web", "java $JAVA_OPTS -jar webapp-runner.jar $WEBAPP_RUNNER_OPTS --port $PORT ./" + relativize(warFile));

    return processTypes;
  }

  @Override
  public void logInfo(String message) { System.out.println(message); }

  public static void main(String[] args) throws Exception {
    String warFile = System.getProperty("heroku.warFile", null);
    String appName = System.getProperty("heroku.appName", null);
    String jdkVersion = System.getProperty("heroku.jdkVersion", null);
    String jdkUrl = System.getProperty("heroku.jdkUrl", null);
    String stack = System.getProperty("heroku.stack", "cedar-14");
    String webappRunnerUrl = System.getProperty("heroku.webappRunnerUrl", WEBAPP_RUNNER_URL);
    String slugFileName = System.getProperty("heroku.slugFileName", "slug.tgz");

    if (warFile == null) {
      throw new IllegalArgumentException("Path to WAR file must be provided with heroku.warFile system property!");
    }
    if (appName == null) {
      throw new IllegalArgumentException("Heroku app name must be provided with heroku.appName system property!");
    }

    (new DeployWar(appName, new File(warFile), new URL(webappRunnerUrl))).
        deploy(new ArrayList<File>(), new HashMap<String, String>(), jdkUrl == null ? jdkVersion : jdkUrl, stack, slugFileName);
  }
}
