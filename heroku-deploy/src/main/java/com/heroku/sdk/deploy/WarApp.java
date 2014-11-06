package com.heroku.sdk.deploy;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarApp extends App {

  private File warFile;

  private File webappRunnerJar;

  public WarApp(String name, File warFile, File webappRunnerJar) {
    super(name);
    this.warFile = warFile;
    this.webappRunnerJar = webappRunnerJar;
  }

  public WarApp(String name, File warFile, File webappRunnerJar, File rootDir, File targetDir) {
    super(name, rootDir, targetDir);
    this.warFile = warFile;
    this.webappRunnerJar = webappRunnerJar;
  }

  public void deploy(List<File> includedFiles, Map<String,String> configVars, String jdkVersion, String jdkUrl) throws Exception {
    Map<String,String> processTypes = new HashMap<String, String>();
    processTypes.put("web", "java $JAVA_OPTS -jar " + relativize(webappRunnerJar) + " --port $PORT " + relativize(warFile));

    includedFiles.add(webappRunnerJar);
    includedFiles.add(warFile);
    super.deploy(includedFiles, configVars, jdkVersion, jdkUrl, processTypes);
  }

  public static void main(String[] args) {
    // init from cmd args
  }
}
