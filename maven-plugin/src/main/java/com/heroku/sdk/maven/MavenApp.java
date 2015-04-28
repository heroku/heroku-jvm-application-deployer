package com.heroku.sdk.maven;

import com.heroku.sdk.deploy.App;
import com.heroku.sdk.deploy.BuildsApp;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MavenApp extends App {

  private Log log;

  public MavenApp(String name, File rootDir, File targetDir, Log log) {
    super("heroku-maven-plugin", name, rootDir, targetDir);
    this.log = log;
  }

  @Override
  protected void prepare(List<File> includedFiles) throws IOException {
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
