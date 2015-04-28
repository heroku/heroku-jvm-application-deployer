package com.heroku.sdk.deploy;

import com.heroku.sdk.deploy.utils.Logger;
import com.heroku.sdk.deploy.utils.UploadListener;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import sun.misc.BASE64Encoder;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class App implements Logger {

  protected Deployer deployer;

  protected String name;

  public App(String name) throws IOException {
    this("heroku-deploy", name, new File(System.getProperty("user.dir")), createTempDir());
  }

  public App(String buildPackDesc, String name, File rootDir, File targetDir) {
    // TODO determine the right deployer
    this.deployer = new SlugDeployer(buildPackDesc, name, rootDir, targetDir, this);
  }

  public void logInfo(String message) { /* nothing by default */ }

  public void logDebug(String message) { /* nothing by default */ }

  public void logWarn(String message) { /* nothing by default */ }

  public String getName() {
    return this.name;
  }

  protected void prepare(List<File> includedFiles) throws IOException {
    deployer.prepare(includedFiles);
  }

  protected void deploy(List<File> includedFiles, Map<String, String> configVars, String jdkVersion, URL jdkUrl, String stack, Map<String, String> processTypes, String tarFilename) throws Exception {
    prepare(includedFiles);
    deployer.deploy(configVars, jdkVersion, jdkUrl, stack, processTypes, tarFilename);
  }

  public void deploy(List<File> includedFiles, Map<String, String> configVars, String jdkVersion, String stack, Map<String, String> processTypes, String tarFilename) throws Exception {
    deploy(includedFiles, configVars, jdkVersion, null, stack, processTypes, tarFilename);
  }

  public void deploy(List<File> includedFiles, Map<String, String> configVars, URL jdkUrl, String stack, Map<String, String> processTypes, String tarFilename) throws Exception {
    deploy(includedFiles, configVars, jdkUrl.toString(), jdkUrl, stack, processTypes, tarFilename);
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
}
