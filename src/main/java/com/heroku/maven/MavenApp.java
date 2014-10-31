package com.heroku.maven;

import com.heroku.api.App;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.List;

public class MavenApp extends App {

  private Log log;

  public MavenApp(String name, List<File> includedFiles, File targetDir, File appDir, File herokuDir, Log log) {
    super(name, includedFiles, targetDir, appDir, herokuDir);
    this.log = log;
  }

  public void logInfo(String message) {
    log.info(message);
  }

  public void logDebug(String message) {
    log.debug(message);
  }

}
