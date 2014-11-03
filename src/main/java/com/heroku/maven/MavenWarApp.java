package com.heroku.maven;

import com.heroku.api.WarApp;
import org.apache.maven.plugin.logging.Log;

import java.io.File;

public class MavenWarApp extends WarApp {
  private Log log;

  public MavenWarApp(String name, File warFile, File webappRunnerJar, File rootDir, File targetDir, Log log) {
    super(name, warFile, webappRunnerJar, rootDir, targetDir);
    this.log = log;
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
}
