package com.heroku.sdk.deploy;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarApp extends App {

  private static final String WEBAPP_RUNNER_URL="http://central.maven.org/maven2/com/github/jsimone/webapp-runner/7.0.40.1/webapp-runner-7.0.40.1.jar";

  private File warFile;

  private File webappRunnerJar;

  public WarApp(String name, File warFile, URL webappRunnerUrl) throws IOException {
    super(name);
    this.warFile = warFile;

    this.webappRunnerJar = new File(getAppDir(), "webapp-runner.jar");
    FileUtils.copyURLToFile(webappRunnerUrl, webappRunnerJar);
  }

  public WarApp(String buildPackDesc, String name, File warFile, File webappRunnerJar, File rootDir, File targetDir) {
    super(buildPackDesc, name, rootDir, targetDir);
    this.warFile = warFile;
    this.webappRunnerJar = webappRunnerJar;
  }

  public void deploy(List<File> includedFiles, Map<String,String> configVars, String jdkVersion, String stack, String slugFileName) throws Exception {
    includedFiles.add(webappRunnerJar);
    includedFiles.add(warFile);
    super.deploy(includedFiles, configVars, jdkVersion, stack, defaultProcTypes(), slugFileName);
  }

  public void deploy(List<File> includedFiles, Map<String,String> configVars, URL jdkUrl, String stack, String slugFileName) throws Exception {
    includedFiles.add(webappRunnerJar);
    includedFiles.add(warFile);
    super.deploy(includedFiles, configVars, jdkUrl, stack, defaultProcTypes(), slugFileName);
  }

  private Map<String,String> defaultProcTypes() {
    Map<String,String> processTypes = new HashMap<String, String>();
    processTypes.put("web", "java $JAVA_OPTS -jar " + relativize(webappRunnerJar) + " $WEBAPP_RUNNER_OPTS --port $PORT " + relativize(warFile));

    return processTypes;
  }

  public static void main(String[] args) throws Exception {
    String warFile = System.getProperty("heroku.warFile", null);
    String appName = System.getProperty("heroku.appName", null);
    String jdkVersion = System.getProperty("heroku.jdkVersion", null);
    String jdkUrl = System.getProperty("heroku.jdkUrl", null);
    String stack = System.getProperty("heroku.stack", "cedar-14");
    String webappRunnerUrl = System.getProperty("heroku.webappRunnerUrl", WEBAPP_RUNNER_URL);
    String slugFileName = System.getProperty("heroku.slugFileName");

    if (warFile == null) {
      throw new IllegalArgumentException("Path to WAR file must be provided with heroku.warFile system property!");
    }
    if (appName == null) {
      throw new IllegalArgumentException("Heroku app name must be provided with heroku.appName system property!");
    }

    (new WarApp(appName, new File(warFile), new URL(webappRunnerUrl))).
        deploy(new ArrayList<File>(), new HashMap<String, String>(), jdkUrl == null ? jdkVersion : jdkUrl, stack, slugFileName);
  }
}
