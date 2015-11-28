package com.heroku.sdk.deploy;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class DeployWar extends WarApp {

  public static final String DEFAULT_WEBAPP_RUNNER_VERSION = "8.0.23.0";

  private static final String WEBAPP_RUNNER_URL_FORMAT="http://central.maven.org/maven2/com/github/jsimone/webapp-runner/%s/webapp-runner-%s.jar";

  public DeployWar(String name, File warFile, URL webappRunnerUrl) throws IOException {
    super(name);
    this.warFile = warFile;

    this.webappRunnerJar = new File(getAppDir(), "webapp-runner.jar");
    FileUtils.copyURLToFile(webappRunnerUrl, webappRunnerJar);
  }

  public DeployWar(String name, File warFile, URL webappRunnerUrl, String apiKey) throws IOException {
      this(name, warFile, webappRunnerUrl);
      this.deployer.setEncodedApiKey(apiKey);
  }
  
  @Override
  protected Map<String,String> defaultProcTypes() {
    Map<String,String> processTypes = new HashMap<String, String>();
    processTypes.put("web", "java $JAVA_OPTS -jar webapp-runner.jar ${WEBAPP_RUNNER_OPTS:-\"--expand-war\"} --port $PORT ./" + relativize(warFile));

    return processTypes;
  }

  private static List<File> includesToList(String includes) {
    List<String> includeStrings = Arrays.asList(includes.split(File.pathSeparator));

    List<File> includeFiles = new ArrayList<>(includeStrings.size());
    for (String includeString : includeStrings) {
      if (!includeString.isEmpty()) {
        includeFiles.add(new File(includeString));
      }
    }

    return includeFiles;
  }

  @Override
  public void logInfo(String message) { System.out.println(message); }

  public static void main(String[] args) throws Exception {
    String warFile = System.getProperty("heroku.warFile", null);
    String appName = System.getProperty("heroku.appName", null);
    String jdkVersion = System.getProperty("heroku.jdkVersion", null);
    String jdkUrl = System.getProperty("heroku.jdkUrl", null);
    String stack = System.getProperty("heroku.stack", "cedar-14");
    List<File> includes = includesToList(System.getProperty("heroku.includes", ""));
    String slugFileName = System.getProperty("heroku.slugFileName", "slug.tgz");

    String webappRunnerVersion = System.getProperty(
            "heroku.webappRunnerVersion", DEFAULT_WEBAPP_RUNNER_VERSION);
    String webappRunnerUrl = System.getProperty(
            "heroku.webappRunnerUrl", String.format(WEBAPP_RUNNER_URL_FORMAT, webappRunnerVersion, webappRunnerVersion));

    if (warFile == null) {
      throw new IllegalArgumentException("Path to WAR file must be provided with heroku.warFile system property!");
    }
    if (appName == null) {
      throw new IllegalArgumentException("Heroku app name must be provided with heroku.appName system property!");
    }

    (new DeployWar(appName, new File(warFile), new URL(webappRunnerUrl))).
        deploy(includes, new HashMap<String, String>(), jdkUrl == null ? jdkVersion : jdkUrl, stack, slugFileName);
  }
}
