package com.heroku.sdk.deploy;

import com.heroku.sdk.deploy.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App implements Logger  {

  protected Deployer deployer;

  protected String name;

  public App(String name) throws IOException {
    this("heroku-deploy", name, new File(System.getProperty("user.dir")), createTempDir(), new ArrayList<String>());
  }

  public App(String client, String name, File rootDir, File targetDir, List<String> buildpacks) {
    this.deployer = new BuildsDeployer(client, name, rootDir, targetDir, buildpacks, this);
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

  protected void prepare(List<File> includedFiles, Map<String, String> processTypes) throws IOException {
    deployer.prepare(includedFiles, processTypes);
  }

  protected void deploy(List<File> includedFiles, Map<String, String> configVars, String jdkVersion, URL jdkUrl, String stack, Map<String, String> processTypes, String tarFilename) throws Exception {
    prepare(includedFiles, processTypes);
    deployer.deploy(configVars, jdkVersion, jdkUrl, stack, processTypes, tarFilename);
  }

  public void deploy(List<File> includedFiles, Map<String, String> configVars, String jdkVersion, String stack, Map<String, String> processTypes, String tarFilename) throws Exception {
    deploy(includedFiles, configVars, jdkVersion, null, stack, processTypes, tarFilename);
  }

  public void deploy(List<File> includedFiles, Map<String, String> configVars, URL jdkUrl, String stack, Map<String, String> processTypes, String tarFilename) throws Exception {
    deploy(includedFiles, configVars, jdkUrl.toString(), jdkUrl, stack, processTypes, tarFilename);
  }

  protected void deploySlug(List<File> includedFiles, Map<String, String> configVars, String jdkVersion, URL jdkUrl, String stack, Map<String, String> processTypes, String tarFilename) throws Exception {
    deployer = new SlugDeployer(deployer.getClient(), getName(), getRootDir(), deployer.getTargetDir(), this);
    deploy(includedFiles, configVars, jdkVersion, jdkUrl, stack, processTypes, tarFilename);
  }

  public void deploySlug(List<File> includedFiles, Map<String, String> configVars, String jdkVersion, String stack, Map<String, String> processTypes, String tarFilename) throws Exception {
    deploySlug(includedFiles, configVars, jdkVersion, null, stack, processTypes, tarFilename);
  }

  public void deploySlug(List<File> includedFiles, Map<String, String> configVars, URL jdkUrl, String stack, Map<String, String> processTypes, String tarFilename) throws Exception {
    deploySlug(includedFiles, configVars, jdkUrl.toString(), jdkUrl, stack, processTypes, tarFilename);
  }

  public void releaseSlug(String slugFilename, Map<String, String> processTypes, Map<String, String> configVars, String stack) throws Exception {
    SlugDeployer slugDeployer = new SlugDeployer(deployer.getClient(), getName(), getRootDir(), deployer.getTargetDir(), this);
    deployer = slugDeployer;
    slugDeployer.releaseSlug(slugFilename, processTypes, configVars, stack);
  }

  protected void createSlug(String slugFilename, List<File> includedFiles, String jdkVersion, URL jdkUrl, String stack) throws Exception {
    SlugDeployer slugDeployer = new SlugDeployer(deployer.getClient(), getName(), getRootDir(), deployer.getTargetDir(), this);
    deployer = slugDeployer;
    prepare(includedFiles, new HashMap<String, String>());
    slugDeployer.createSlug(slugFilename, jdkVersion, jdkUrl, stack);
  }

  public void createSlug(String slugFilename, List<File> includedFiles, String jdkVersion, String stack) throws Exception {
    createSlug(slugFilename, includedFiles, jdkVersion, null, stack);
  }

  public void createSlug(String slugFilename, List<File> includedFiles, URL jdkUrl, String stack) throws Exception {
    createSlug(slugFilename, includedFiles, jdkUrl.toString(), jdkUrl, stack);
  }

  protected static File createTempDir() throws IOException {
    return Files.createTempDirectory("heroku-deploy").toFile();
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
}
