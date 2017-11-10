package com.heroku.sdk.deploy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.heroku.sdk.deploy.utils.Main;

/**
 * An easy way to deploy an executable Jar file
 */
public class DeployJar extends App {

  protected File jarFile;

  protected String jarOpts;

  public DeployJar(String name, File jarFile, String jarOpts, List<String> buildpacks) throws IOException {
    super(name, buildpacks);
    this.jarFile = jarFile;
    this.jarOpts = jarOpts;
  }

  @Override
  public void deploy(List<File> includedFiles, Map<String,String> configVars, String jdkVersion, Map<String,String> userDefinedProcessTypes, String slugFileName) throws Exception {
    includedFiles.add(jarFile);

    Map<String,String> processTypes = getProcfile();
    if (processTypes.isEmpty()) {
      processTypes.put("web", "java $JAVA_OPTS -jar " + relativize(jarFile) + " " + jarOpts + " $JAR_OPTS");
    }

    super.deploy(includedFiles, configVars, jdkVersion, processTypes, slugFileName);
  }

  private Map<String, String> getProcfile() {
    Map<String, String> procTypes = new HashMap<String, String>();

    File procfile = new File("Procfile");
    if (procfile.exists()) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(procfile));
        String line = reader.readLine();
        while (line != null) {
          if (line.contains(":")) {
            Integer colon = line.indexOf(":");
            String key = line.substring(0, colon);
            String value = line.substring(colon + 1);
            procTypes.put(key.trim(), value.trim());
          }
          line = reader.readLine();
        }
      } catch (Exception e) {
        logDebug(e.getMessage());
      }
    }

    return procTypes;
  }

  @Override
  public void logInfo(String message) { System.out.println(message); }

  @Override
  public void logDebug(String message) {
    if (Main.isDebug()) {
      System.out.println(message);
    }
  }

  public static void deploy() throws Exception {
    final String jarFile = System.getProperty("heroku.jarFile", null);
    final String jarOpts = System.getProperty("heroku.jarOpts", "");

    if (jarFile == null) {
      throw new IllegalArgumentException("Path to JAR file must be provided with heroku.jarFile system property!");
    }

    Main.deploy(new Main.DeployFunction<String, List<String>, App>() {
      @Override
      public App apply(String appName, List<String> buildpacks) throws IOException {
        return new DeployJar(appName, new File(jarFile), jarOpts, buildpacks);
      }
    });
  }

  public static void main(String[] args) {
    try {
      deploy();
    } catch (Exception e) {
      System.out.println(" ! ERROR: " + e.getMessage());
      if (Main.isDebug()) {
        e.printStackTrace();
      } else {
        System.out.println(" !        Re-run with HEROKU_DEBUG=1 for more info.");
      }
      System.exit(1);
    }
  }
}
