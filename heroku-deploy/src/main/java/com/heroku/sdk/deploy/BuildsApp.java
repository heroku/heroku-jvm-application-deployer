package com.heroku.sdk.deploy;

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

public class BuildsApp extends App {

  private static String JVM_BUILDPACK_URL="https://codon-buildpacks.s3.amazonaws.com/buildpacks/heroku/jvm-common.tgz";

  public BuildsApp(String name, File rootDir, File targetDir) {
    super("builds-api", name, rootDir, targetDir);
  }

  protected void deploy(List<File> includedFiles, Map<String, String> configVars, String jdkVersion, URL jdkUrl, String stack, Map<String, String> processTypes, String tarFilename) throws Exception {
    prepare(includedFiles);
    mergeConfigVars(configVars);
    deploySource(stack, processTypes, buildSourceTar(tarFilename));
  }

  protected void createSlug(String tarFilename, List<File> includedFiles, String jdkVersion, URL jdkUrl, String stack) throws Exception {
    prepare(includedFiles);
    buildSourceTar(tarFilename);
  }

  protected void addExtras() throws IOException {
    Files.write(
        Paths.get(new File(getAppDir(), "Procfile").getPath()),
        ("web: java $JAVA_OPTS -cp target/classes:target/dependency/* Main").getBytes(StandardCharsets.UTF_8)
    );
  }

  protected File buildSourceTar(String tarFilename)
      throws InterruptedException, ArchiveException, IOException {
    logInfo("---> Creating build...");
    try {
      FileUtils.forceDelete(new File(getHerokuDir(), tarFilename));
    } catch (IOException e) { /* no-op */ }

    // NOTE Big difference here!
    File tarFile = Tar.create(tarFilename, "./", getAppDir(), getHerokuDir());
    Long size = tarFile.length() / (1024 * 1024);
    if (size == 0l) size = 1l;

    logInfo("     - file: " + relativize(tarFile));
    logInfo("     - size: " + size + "MB");
    return tarFile;
  }

  protected Builds deploySource(String stack, Map<String, String> processTypes, File tarFile)
      throws IOException, ArchiveException, InterruptedException {
    Builds builds = new Builds(name, stack, parseCommit(), getEncodedApiKey());

    logInfo("---> Setting buildpack...");
    logInfo("     - name: jvm-common");
    builds.setBuildpack(JVM_BUILDPACK_URL);

    Map sourceResponse = builds.createSource();
    logDebug("Heroku Source response: " + sourceResponse);
    logDebug("Heroku Blob URL: " + builds.getBlobUrl());

    logInfo("---> Uploading build...");
    builds.upload(tarFile, this);

    builds.build(new RestClient.OutputLogger() {
      @Override
      public void log(String line) {
        logInfo("remote: " + line);
      }
    });

    logInfo("---> Done");

    return builds;
  }
}
