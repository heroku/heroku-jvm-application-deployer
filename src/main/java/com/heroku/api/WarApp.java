package com.heroku.api;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarApp extends App {

  private File warFile;

  public WarApp(String name, File warFile) {
    super(name);
    this.warFile = warFile;
  }

  public WarApp(String name, File warFile, File rootDir, File targetDir) {
    super(name, rootDir, targetDir);
    this.warFile = warFile;
  }

  public void deploy(List<File> includedFiles, Map<String,String> configVars, String jdkVersion, String jdkUrl) throws Exception {
    Map<String,String> processTypes = new HashMap<String, String>();
    processTypes.put("web", "java -jar webapp-runner.jar " + warFile.getName());

    super.deploy(includedFiles, configVars, jdkVersion, jdkUrl, processTypes);
  }

  @Override
  public void prepare(List<File> includedFiles, String jdkVersion, String jdkUrl) throws Exception {
    // install webapp-runner
  }

  public static void main(String[] args) {
    // init from cmd args
  }
}
