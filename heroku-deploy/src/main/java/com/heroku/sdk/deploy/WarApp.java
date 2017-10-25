package com.heroku.sdk.deploy;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarApp extends App {

  protected File warFile;

  protected File webappRunnerJar;

  public WarApp(String name) throws IOException {
    super(name);
  }

  public WarApp(String name, List<String> buildpacks) throws IOException {
    super(name, buildpacks);
  }

  public WarApp(String client, String name, File warFile, File webappRunnerJar, File rootDir, File targetDir, List<String> buildpacks) {
    super(client, name, rootDir, targetDir, buildpacks);
    this.warFile = warFile;
    this.webappRunnerJar = webappRunnerJar;
  }

  @Override
  public void deploy(List<File> includedFiles, Map<String,String> configVars, String jdkVersion, String stack, String buildFilename) throws Exception {
    includedFiles.add(webappRunnerJar);
    includedFiles.add(warFile);
    super.deploy(includedFiles, configVars, jdkVersion, stack, buildFilename);
  }

  @Override
  protected Map<String,String> defaultProcTypes() {
    Map<String,String> processTypes = new HashMap<>();
    processTypes.put("web", "java $JAVA_OPTS -jar " + relativize(webappRunnerJar) + " $WEBAPP_RUNNER_OPTS --port $PORT " + relativize(warFile));

    return processTypes;
  }
}
