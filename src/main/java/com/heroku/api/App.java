package com.heroku.api;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import sun.misc.BASE64Encoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

  private static Map<String,String> jdkUrlStrings = new HashMap<String,String>();

  static {
    jdkUrlStrings.put("1.6", "https://lang-jvm.s3.amazonaws.com/jdk/openjdk1.6-latest.tar.gz");
    jdkUrlStrings.put("1.7", "https://lang-jvm.s3.amazonaws.com/jdk/openjdk1.7-latest.tar.gz");
    jdkUrlStrings.put("1.8", "https://lang-jvm.s3.amazonaws.com/jdk/openjdk1.8-latest.tar.gz");
  }

  private String name;

  private List<File> includedFiles;

  private File targetDir;

  private File appDir;

  private File herokuDir;

  private String encodedApiKey = null;

  public void logInfo(String message) { /* nothing by default */ }

  public void logDebug(String message) { /* nothing by default */ }

  public App(String name, List<File> includedFiles, File targetDir, File appDir, File herokuDir) {
    this.name = name;
    this.includedFiles = includedFiles;
    this.targetDir = targetDir;
    this.appDir = appDir;
    this.herokuDir = herokuDir;
  }

  public void prepare(String jdkVersion, String jdkUrl) throws Exception {
    logInfo("---> Packaging application...");
    logInfo("     - app: " + name);

    try {
      for (File file : includedFiles) {
        logInfo("     - including: ./" + relativize(targetDir.getParentFile(), file));
        FileUtils.copyDirectory(file, new File(appDir, FilenameUtils.getBaseName(file.getPath())));
      }
    } catch (IOException ioe) {
      throw new Exception("There was an error packaging the application for deployment.", ioe);
    }

    // configVars

    try {
      if (jdkUrl == null) {
        logInfo("     - installing: OpenJDK " + jdkVersion);
        vendorJdk(new URL(jdkUrlStrings.get(jdkVersion)));
      } else {
        logInfo("     - installing: " + jdkUrl);
        vendorJdk(new URL(jdkUrl));
      }
    } catch (Exception e) {
      throw new Exception("There was an error downloading the JDK.", e);
    }
  }

  public Slug deploy(Map<String,String> configVars, Map<String,String> processTypes) throws IOException, Curl.CurlException, ArchiveException, InterruptedException {
    Slug slug = new Slug(name, getEncodedApiKey(), processTypes);
    logDebug("Heroku Slug request: " + slug.getSlugRequest());

    logInfo("---> Creating slug...");
    File slugFile = Tar.create("slug", "./app", herokuDir);
    logInfo("     - file: ./" + relativize(targetDir.getParentFile(), slugFile));
    logInfo("     - size: " + (slugFile.length() / (1024 * 1024)) + "MB");

    // config var stuff...

    Map slugResponse = slug.create();
    logDebug("Heroku Slug response: " + slugResponse);
    logDebug("Heroku Blob URL: " + slug.getBlobUrl());
    logDebug("Heroku Slug Id: " + slug.getSlugId());

    logInfo("---> Uploading slug...");
    slug.upload(slugFile);
    logInfo("     - stack: " + slug.getStackName());
    logInfo("     - process types: " + ((Map) slugResponse.get("process_types")).keySet());

    logInfo("---> Releasing...");
    Map releaseResponse = slug.release();
    logDebug("Heroku Release response: " + releaseResponse);
    logInfo("     - version: " + releaseResponse.get("version"));

    return slug;
  }

  private void vendorJdk(URL jdkUrl) throws IOException, InterruptedException {
    File jdkHome = new File(appDir, ".jdk");
    jdkHome.mkdir();

    File jdkTgz = new File(herokuDir, "jdk-pkg.tar.gz");
    FileUtils.copyURLToFile(jdkUrl, jdkTgz);

    Tar.extract(jdkTgz, jdkHome);
  }

  private String relativize(File base, File path) {
    return base.toURI().relativize(path.toURI()).getPath();
  }

  private String getEncodedApiKey() throws IOException {
    if (encodedApiKey == null) {
      String apiKey = System.getenv("HEROKU_API_KEY");
      if (null == apiKey || apiKey.equals("")) {
        ProcessBuilder pb = new ProcessBuilder().command("heroku", "auth:token");
        Process p = pb.start();

        BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        apiKey = "";
        while ((line = bri.readLine()) != null) {
          apiKey += line;
        }
      }
      encodedApiKey = new BASE64Encoder().encode((":" + apiKey).getBytes());
    }
    return encodedApiKey;
  }
}
