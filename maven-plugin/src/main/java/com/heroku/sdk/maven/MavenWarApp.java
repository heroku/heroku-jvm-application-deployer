package com.heroku.sdk.maven;

import com.heroku.sdk.deploy.WarApp;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.net.URL;
import java.util.List;

public class MavenWarApp extends WarApp {
  private Log log;

  public MavenWarApp(String name, File warFile, File webappRunnerJar, File rootDir, File targetDir, Log log) {
    super("heroku-maven-plugin", name, warFile, webappRunnerJar, rootDir, targetDir);
    this.log = log;
  }

  @Override
  protected void prepare(List<File> includedFiles) throws Exception {
    super.prepare(includedFiles);

    FileUtils.copyFile(new File(getRootDir(), "pom.xml"), new File(getAppDir(), "pom.xml"));
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
