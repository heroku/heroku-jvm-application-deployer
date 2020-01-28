package com.heroku.sdk.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;

public class MavenWarApp {
  private Log log;

  private boolean isUploadProgressEnabled;

  public MavenWarApp(String name, File warFile, File webappRunnerJar, File rootDir, File targetDir, Log log, boolean logProgress, String[] buildpacks) {
    //super("heroku-maven-plugin", name, warFile, webappRunnerJar, rootDir, targetDir, Arrays.asList(buildpacks));
    this.isUploadProgressEnabled = logProgress;
    this.log = log;
  }

  public void logUploadProgress(Long uploaded, Long contentLength) {
    logInfo("[" + uploaded + "/" + contentLength + "]");
  }

  public Boolean isUploadProgressEnabled() {
    return isUploadProgressEnabled;
  }

  protected void prepare(List<File> includedFiles, Map<String, String> processTypes) throws IOException {
    /*super.prepare(includedFiles, processTypes);

    File appTargetDir = new File (getAppDir(), "target");
    FileUtils.forceMkdir(appTargetDir);
    FileUtils.copyFile(
            new File(getTargetDir(), ListDependencies.FILENAME),
            new File(appTargetDir, ListDependencies.FILENAME));
    FileUtils.copyFile(new File(getRootDir(), "pom.xml"), new File(getAppDir(), "pom.xml"));

     */
  }

  public void logInfo(String message) {
    log.info(message);
  }

  public void logDebug(String message) {
    log.debug(message);
  }

  public void logWarn(String message) {
    log.warn(message);
  }

  public void logError(String message) {
    log.error(message);
  }
}
