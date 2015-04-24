package com.heroku.sdk.deploy;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * An easy way to deploy an executable Jar file
 */
public class DeployJar extends App {

  protected File jarFile;

  public DeployJar(String name, File jarFile) throws IOException {
    super(name);
    this.jarFile = jarFile;
  }

  public void deploy(List<File> includedFiles, Map<String,String> configVars, String jdkVersion, String stack, String slugFileName) throws Exception {
    includedFiles.add(jarFile);
    super.deploy(includedFiles, configVars, jdkVersion, stack, defaultProcTypes(), slugFileName);
  }

  public void deploy(List<File> includedFiles, Map<String,String> configVars, URL jdkUrl, String stack, String slugFileName) throws Exception {
    includedFiles.add(jarFile);
    super.deploy(includedFiles, configVars, jdkUrl, stack, defaultProcTypes(), slugFileName);
  }

  protected Map<String,String> defaultProcTypes() {
    Map<String,String> processTypes = new HashMap<>();
    processTypes.put("web", "java $JAVA_OPTS -jar " + relativize(jarFile));

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
    String warFile = System.getProperty("heroku.jarFile", null);
    String appName = System.getProperty("heroku.appName", null);
    String jdkVersion = System.getProperty("heroku.jdkVersion", null);
    String jdkUrl = System.getProperty("heroku.jdkUrl", null);
    String stack = System.getProperty("heroku.stack", "cedar-14");
    List<File> includes = includesToList(System.getProperty("heroku.includes", ""));

    String slugFileName = System.getProperty("heroku.slugFileName", "slug.tgz");

    if (warFile == null) {
      throw new IllegalArgumentException("Path to WAR file must be provided with heroku.warFile system property!");
    }
    if (appName == null) {
      throw new IllegalArgumentException("Heroku app name must be provided with heroku.appName system property!");
    }

    (new DeployJar(appName, new File(warFile))).
        deploy(includes, new HashMap<String, String>(), jdkUrl == null ? jdkVersion : jdkUrl, stack, slugFileName);
  }
}
