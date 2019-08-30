package com.heroku.sdk.deploy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.heroku.sdk.deploy.utils.Logger;

public class App implements Logger  {

  protected Deployer deployer;

  protected String name;

  public App(String name) throws IOException {
    this(name, new ArrayList<String>());
  }

  public App(String name, List<String> buildpacks) throws IOException {
    this("heroku-deploy", name, new File(System.getProperty("user.dir")), createTempDir(), buildpacks);
  }

  public App(String client, String name, File rootDir, File targetDir, List<String> buildpacks) {
    this.deployer = new Deployer(client, name, rootDir, targetDir, buildpacks, this);
  }

  @Override
  public void logInfo(String message) { /* nothing by default */ }

  @Override
  public void logDebug(String message) { /* nothing by default */ }

  @Override
  public void logWarn(String message) { /* nothing by default */ }

  @Override
  public void logError(String message) { /* nothing by default */ }

  @Override
  public void logUploadProgress(Long uploaded, Long contentLength) {
    logDebug("Uploaded " + uploaded + "/" + contentLength);
  }

  @Override
  public Boolean isUploadProgressEnabled() {
    return false;
  }

  public String getName() {
    return deployer.getName();
  }

  public void deploy(List<File> includedFiles, Map<String, String> configVars, String jdkVersion, Map<String, String> userDefinedProcessTypes, String tarFilename) throws Exception {
    Map<String,String> processTypes = defaultProcTypes();
    processTypes.putAll(userDefinedProcessTypes);
    prepare(includedFiles, processTypes);
    deployer.deploy(configVars, jdkVersion, tarFilename);
  }

  protected void prepare(List<File> includedFiles, Map<String, String> processTypes) throws IOException {
    deployer.prepare(includedFiles, processTypes);
  }

  protected static File createTempDir() throws IOException {
    File f = Files.createTempDirectory("heroku-deploy").toFile();
    deleteTemporaryDirectoryOnShutdownHook(f.toPath());
    return f;
  }

  private static void deleteTemporaryDirectoryOnShutdownHook(final Path path) {
    final Runnable runnable = new DeleteDirectoryRunnable(path);
    Runtime.getRuntime().addShutdownHook(new Thread(runnable));
  }

  protected String relativize(File path) {
    return deployer.relativize(path);
  }

  protected File getAppDir() {
    return deployer.getAppDir();
  }

  protected File getRootDir() {
    return deployer.getRootDir();
  }

  protected File getTargetDir() { return deployer.getTargetDir(); }

  protected Map<String,String> defaultProcTypes() {
    return new HashMap<>();
  }
}
