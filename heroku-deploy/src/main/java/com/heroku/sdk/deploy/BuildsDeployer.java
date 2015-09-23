package com.heroku.sdk.deploy;

import com.heroku.sdk.deploy.endpoints.Builds;
import com.heroku.sdk.deploy.utils.Logger;
import com.heroku.sdk.deploy.utils.RestClient;
import com.heroku.sdk.deploy.utils.Tar;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class BuildsDeployer extends Deployer {

  private List<String> buildpacks;

  public BuildsDeployer(String notUsed, String name, File rootDir, File targetDir, List<String> buildpacks, Logger logger) {
    super("builds-api", name, rootDir, targetDir, logger);
    this.buildpacks = buildpacks;
  }

  @Override
  protected void addExtras(Map<String, String> processTypes) throws IOException {
    addProcfile(processTypes);
    addJdkOverlay();
  }

  private void addProcfile(Map<String, String> processTypes) throws IOException {
    Map<String, String> allProcessTypes = getProcfile();
    allProcessTypes.putAll(processTypes);
    if (allProcessTypes.isEmpty()) logWarn("No processTypes specified!");

    String procfile = "";

    for (String key : allProcessTypes.keySet()) {
      procfile += key + ": " + allProcessTypes.get(key) + "\n";
    }

    logDebug("Procfile:\n===================\n" + procfile + "\n===================");

    Files.write(
        Paths.get(new File(getAppDir(), "Procfile").getPath()), (procfile).getBytes(StandardCharsets.UTF_8)
    );
  }

  private void addJdkOverlay() throws IOException {
    File jdkOverlayDir = new File(getRootDir(), ".jdk-overlay");
    File toJdkOverlayDir = new File(getAppDir(), ".jdk-overlay");
    if (jdkOverlayDir.exists()) {
      logInfo("       - including JDK overlay");
      FileUtils.copyDirectory(jdkOverlayDir, toJdkOverlayDir);
    }
  }

  protected void vendorJdk(String jdkVersion, URL jdkUrl, String stackName) throws IOException, InterruptedException, ArchiveException {
    String realJdkVersion = jdkVersion == null ? getJdkVersion() : jdkVersion;
    if (realJdkVersion != null) {
      Files.write(
          Paths.get(new File(getAppDir(), "system.properties").getPath()),
          ("java.runtime.version=" + realJdkVersion).getBytes(StandardCharsets.UTF_8)
      );
    } else if (jdkUrl != null) {
      logWarn("JDK URL is not supported with partial slug deployment! Ignoring...");
    }
  }

  protected File buildSlugFile(String tarFilename)
      throws InterruptedException, ArchiveException, IOException {
    logInfo("-----> Creating build...");
    try {
      FileUtils.forceDelete(new File(getHerokuDir(), tarFilename));
    } catch (IOException e) { /* no-op */ }

    // NOTE Big difference here!
    File tarFile = Tar.create(tarFilename, "./", getAppDir(), getHerokuDir());
    Long size = tarFile.length() / (1024 * 1024);
    if (size == 0l) size = 1l;

    logInfo("       - file: " + relativize(tarFile));
    logInfo("       - size: " + size + "MB");
    return tarFile;
  }

  protected void deploySlug(String stack, Map<String, String> processTypes, File tarFile)
      throws IOException, ArchiveException, InterruptedException {
    Builds builds = new Builds(name, stack, parseCommit(), getEncodedApiKey(), buildpacks);

    Map sourceResponse = builds.createSource();
    logDebug("Heroku Source response: " + sourceResponse);
    logDebug("Heroku Blob URL: " + builds.getBlobUrl());

    logInfo("-----> Uploading build...");
    builds.upload(tarFile, logger);
    logInfo("       - success");

    logInfo("-----> Deploying...");
    Map buildInfo = builds.build(new RestClient.OutputLogger() {
      @Override
      public void log(String line) {
        logInfo("remote: " + line);
      }
    });

    if (!"succeeded".equals(buildInfo.get("status"))) {
      logDebug("Failed Build ID: " + buildInfo.get("id"));
      logDebug("Failed Build Status: " + buildInfo.get("status"));
      logDebug("Failed Build UpdatedAt: " + buildInfo.get("updated_at"));
      throw new RuntimeException("The build failed");
    }
  }
}
