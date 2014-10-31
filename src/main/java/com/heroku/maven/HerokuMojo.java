package com.heroku.maven;

import com.heroku.api.Curl;
import com.heroku.api.Slug;
import com.heroku.api.Tar;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import sun.misc.BASE64Encoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deploys an application to Heroku
 *
 * @goal deploy
 * @execute phase="package"
 */
public class HerokuMojo extends AbstractMojo {

  private static Map<String,String> jdkUrlStrings = new HashMap<String,String>();

  static {
    jdkUrlStrings.put("1.6", "https://lang-jvm.s3.amazonaws.com/jdk/openjdk1.6-latest.tar.gz");
    jdkUrlStrings.put("1.7", "https://lang-jvm.s3.amazonaws.com/jdk/openjdk1.7-latest.tar.gz");
    jdkUrlStrings.put("1.8", "https://lang-jvm.s3.amazonaws.com/jdk/openjdk1.8-latest.tar.gz");
  }

  /**
   * The maven project.
   *
   * @parameter property="project"
   * @readonly
   */
  private MavenProject project;

  /**
   * @parameter property="project.build.directory"
   * @readonly
   */
  private File outputPath;

  /**
   * The name of the Heroku app.
   * <br/>
   * Command line -Dheroku.appName=...
   *
   * @required
   * @parameter property="heroku.appName"
   */
  protected String appName = null;

  /**
   * The major version of the JDK Heroku with run the app with.
   * <br/>
   * Command line -Dheroku.jdkVersion=...
   *
   * @parameter property="heroku.jdkVersion"
   *            default-value="1.7"
   */
  protected String jdkVersion = null;

  /**
   * The URL of the JDK binaries Heroku will use.
   * <br/>
   * Command line -Dheroku.jdkUrl=...
   *
   * @parameter property="heroku.jdkUrl"
   */
  protected String jdkUrl = null;

  /**
   * Configuration variables that will be set on the Heroku app.
   *
   * @parameter property="heroku.configVars"
   */
  protected Map<String,String> configVars = null;

  /**
   * The process types used to run on Heroku (similar to Procfile).
   *
   * @required
   * @parameter property="heroku.processTypes"
   */
  protected Map<String,String> processTypes = null;

  private String encodedApiKey = null;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("---> Packaging application...");
    getLog().info("     - app: " + appName);

    getTargetDir().mkdirs();
    getHerokuDir().mkdir();
    getAppDir().mkdir();

    List<File> includedDirs = new ArrayList<File>();
    includedDirs.add(getTargetDir());

    // build the slug
    try {
      for (File file : includedDirs) {
        getLog().info("     - including: ./" + relativize(getTargetDir().getParentFile(), file));
        FileUtils.copyDirectory(file, new File(getAppDir(), FilenameUtils.getBaseName(file.getPath())));
      }
    } catch (IOException ioe) {
      throw new MojoFailureException("There was an error packaging the application for deployment.", ioe);
    }

    try {
      if (jdkUrl == null) {
        getLog().info("     - installing: OpenJDK " + jdkVersion);
        vendorJdk(getAppDir(), new URL(jdkUrlStrings.get(jdkVersion)));
      } else {
        getLog().info("     - installing: " + jdkUrl);
        vendorJdk(getAppDir(), new URL(jdkUrl));
      }
    } catch (Exception e) {
      throw new MojoFailureException("There was an error downloading the JDK.", e);
    }
//    addSlugExtras();

    // log included files

    // copy included files to getAppDir()

    // add slug extras

    try {
      deploy();
    } catch (Curl.CurlException ce) {
      throw new MojoFailureException(ce.getReponse(), ce);
    } catch (Exception e) {
      throw new MojoFailureException(e.getMessage(), e);
    }
  }

  private void deploy() throws IOException, Curl.CurlException, ArchiveException, InterruptedException {

    Slug slug = new Slug(appName, getEncodedApiKey(), getProcessTypes());
    getLog().debug("Heroku Slug request: " + slug.getSlugRequest());

    getLog().info("---> Creating slug...");
    File slugFile = Tar.create("slug", "./app", getHerokuDir());
    getLog().info("     - file: ./" + relativize(getTargetDir().getParentFile(), slugFile));
    getLog().info("     - size: " + (slugFile.length() / (1024 * 1024)) + "MB");

    // config var stuff...

    Map slugResponse = slug.create();
    getLog().debug("Heroku Slug response: " + slugResponse);
    getLog().debug("Heroku Blob URL: " + slug.getBlobUrl());
    getLog().debug("Heroku Slug Id: " + slug.getSlugId());

    getLog().info("---> Uploading slug...");
    slug.upload(slugFile);
    getLog().info("     - stack: " + slug.getStackName());
    getLog().info("     - process types: " + ((Map)slugResponse.get("process_types")).keySet());

    getLog().info("---> Releasing...");
    Map releaseResponse = slug.release();
    getLog().debug("Heroku Release response: " + releaseResponse);
    getLog().info("     - version: " + releaseResponse.get("version"));
  }

  private void vendorJdk(File appDir, URL jdkUrl) throws IOException, InterruptedException {
    File jdkHome = new File(appDir, ".jdk");
    jdkHome.mkdir();

    File jdkTgz = new File(getHerokuDir(), "jdk-pkg.tar.gz");
    FileUtils.copyURLToFile(jdkUrl, jdkTgz);

    Tar.extract(jdkTgz, jdkHome);
  }

  private File getHerokuDir() {
    return new File(getTargetDir(), "heroku");
  }

  private File getAppDir() {
    return new File(getHerokuDir(), "app");
  }

  private File getTargetDir() {
    return outputPath;
  }

  private Map<String,String> getProcessTypes() {
    if (processTypes.isEmpty()) throw new IllegalArgumentException("Must provide a process type!");
    return processTypes;
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

  private String buildSlug(File targetDir, File appTargetDir, File herokuDir) {

    return "";
  }

  private String relativize(File base, File path) {
    return base.toURI().relativize(path.toURI()).getPath();
  }
}
