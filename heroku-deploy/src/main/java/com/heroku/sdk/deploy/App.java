package com.heroku.sdk.deploy;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FileUtils;
import sun.misc.BASE64Encoder;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class App {

  private static Map<String, Map<String, String>> jdkUrlsByStack = new HashMap<String, Map<String, String>>();

  static {
    Map<String, String> cedarJdkUrlStrings = new HashMap<String, String>();
    cedarJdkUrlStrings.put("1.6", "https://lang-jvm.s3.amazonaws.com/jdk/cedar/openjdk1.6-latest.tar.gz");
    cedarJdkUrlStrings.put("1.7", "https://lang-jvm.s3.amazonaws.com/jdk/cedar/openjdk1.7-latest.tar.gz");
    cedarJdkUrlStrings.put("1.8", "https://lang-jvm.s3.amazonaws.com/jdk/cedar/openjdk1.8-latest.tar.gz");

    Map<String, String> cedar14JdkUrlStrings = new HashMap<String, String>();
    cedar14JdkUrlStrings.put("1.6", "https://lang-jvm.s3.amazonaws.com/jdk/cedar-14/openjdk1.6-latest.tar.gz");
    cedar14JdkUrlStrings.put("1.7", "https://lang-jvm.s3.amazonaws.com/jdk/cedar-14/openjdk1.7-latest.tar.gz");
    cedar14JdkUrlStrings.put("1.8", "https://lang-jvm.s3.amazonaws.com/jdk/cedar-14/openjdk1.8-latest.tar.gz");

    jdkUrlsByStack.put("cedar", cedarJdkUrlStrings);
    jdkUrlsByStack.put("cedar-14", cedar14JdkUrlStrings);
  }

  private String buildPackDesc;

  private String name;

  private File rootDir;

  private File targetDir;

  private String encodedApiKey = null;

  public void logInfo(String message) { /* nothing by default */ }

  public void logDebug(String message) { /* nothing by default */ }

  public void logWarn(String message) { /* nothing by default */ }

  public App(String name) throws IOException {
    this("heroku-deploy", name, new File(System.getProperty("user.dir")), createTempDir());
  }

  public App(String buildPackDesc, String name, File rootDir, File targetDir) {
    this.buildPackDesc = buildPackDesc;
    this.name = getHerokuProperties().getProperty("heroku.appName", name);
    this.rootDir = rootDir;
    this.targetDir = targetDir;

    try {
      FileUtils.forceDelete(getAppDir());
    } catch (IOException e) { /* do nothing */ }

    getHerokuDir().mkdir();
    getAppDir().mkdir();
  }

  public String getName() {
    return this.name;
  }

  protected void deploy(List<File> includedFiles, Map<String, String> configVars, String jdkVersion, URL jdkUrl, String stack, Map<String, String> processTypes, String slugFilename) throws Exception {
    prepare(includedFiles);
    mergeConfigVars(configVars);
    vendorJdk(jdkVersion, jdkUrl, stack);
    createAndReleaseSlug(stack, processTypes, slugFilename);
  }

  public void deploy(List<File> includedFiles, Map<String, String> configVars, String jdkVersion, String stack, Map<String, String> processTypes, String slugFileName) throws Exception {
    deploy(includedFiles, configVars, jdkVersion, null, stack, processTypes, slugFileName);
  }

  public void deploy(List<File> includedFiles, Map<String, String> configVars, URL jdkUrl, String stack, Map<String, String> processTypes, String slugFileName) throws Exception {
    deploy(includedFiles, configVars, jdkUrl.toString(), jdkUrl, stack, processTypes, slugFileName);
  }

  protected void createSlug(String slugFilename, List<File> includedFiles, String jdkVersion, URL jdkUrl, String stack) throws Exception {
    prepare(includedFiles);
    vendorJdk(jdkVersion, jdkUrl, stack);
    buildSlugFile(slugFilename);
  }

  public void createSlug(String slugFilename, List<File> includedFiles, String jdkVersion, String stack) throws Exception {
    createSlug(slugFilename, includedFiles, jdkVersion, null, stack);
  }

  public void createSlug(String slugFilename, List<File> includedFiles, URL jdkUrl, String stack) throws Exception {
    createSlug(slugFilename, includedFiles, jdkUrl.toString(), jdkUrl, stack);
  }

  public void deploySlug(String slugFilename, Map<String, String> processTypes, Map<String, String> configVars, String stack) throws Exception {
    mergeConfigVars(configVars);

    File slugFile = new File(getHerokuDir(), slugFilename);
    if (slugFile.exists()) {
      logInfo("---> Using existing slug...");
      logInfo("     - file: ./" + relativize(slugFile));
      logInfo("     - size: " + (slugFile.length() / (1024 * 1024)) + "MB");
      deploySlug(stack, processTypes, slugFile);
    } else {
      throw new FileNotFoundException("Slug file not found!");
    }
  }

  protected void prepare(List<File> includedFiles) throws IOException {
    logInfo("---> Packaging application...");
    logInfo("     - app: " + name);

    try {
      for (File file : includedFiles) {
        logInfo("     - including: ./" + relativize(file));
        copy(file, new File(getAppDir(), relativize(file)));
      }
      try {
        // this makes sure we don't put an old slug or a cached jdk inside the slug
        FileUtils.forceDelete(new File(getAppDir(), relativize(getHerokuDir())));
      } catch (IOException e) { /* do nothing */ }
      addProfileScript();
    } catch (IOException ioe) {
      throw new IOException("There was an error packaging the application for deployment.", ioe);
    }
  }

  protected void copy(File file, File copyTarget) throws IOException {
    if (file.isDirectory()) {
      Files.walkFileTree(file.toPath(), new CopyFileVisitor(copyTarget.toPath()));
    } else {
      Files.createDirectories(copyTarget.getParentFile().toPath());
      Files.copy(file.toPath(), copyTarget.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }
  }

  protected void mergeConfigVars(Map<String, String> configVars) throws Exception {
    (new ConfigVars(this, getEncodedApiKey())).merge(configVars);
  }

  protected File buildSlugFile(String slugFilename)
      throws InterruptedException, ArchiveException, IOException {
    logInfo("---> Creating slug...");
    try {
      FileUtils.forceDelete(new File(getHerokuDir(), slugFilename));
    } catch (IOException e) { /* no-op */ }
    File slugFile = Tar.create(slugFilename, "./app", getHerokuDir());
    logInfo("     - file: ./" + relativize(slugFile));
    logInfo("     - size: " + (slugFile.length() / (1024 * 1024)) + "MB");
    return slugFile;
  }

  protected Slug createAndReleaseSlug(String stack, Map<String, String> processTypes, String slugFilename)
      throws IOException, Curl.CurlException, ArchiveException, InterruptedException {
    return deploySlug(stack, processTypes, buildSlugFile(slugFilename));
  }

  protected Slug deploySlug(String stack, Map<String, String> processTypes, File slugFile)
      throws IOException, ArchiveException, InterruptedException, Curl.CurlException {
    Map<String, String> allProcessTypes = getProcfile();
    allProcessTypes.putAll(processTypes);
    if (allProcessTypes.isEmpty()) logWarn("No processTypes specified!");

    Slug slug = new Slug(buildPackDesc, name, stack, getEncodedApiKey(), allProcessTypes);
    logDebug("Heroku Slug request: " + slug.getSlugRequest());

    Map slugResponse = slug.create();
    logDebug("Heroku Slug response: " + slugResponse);
    logDebug("Heroku Blob URL: " + slug.getBlobUrl());
    logDebug("Heroku Slug Id: " + slug.getSlugId());

    uploadSlug(slug, slugFile, ((Map) slugResponse.get("process_types")).keySet());

    releaseSlug(slug);

    return slug;
  }

  protected void uploadSlug(Slug slug, File slugFile, Set processTypes)
      throws IOException, Curl.CurlException, ArchiveException, InterruptedException {
    logInfo("---> Uploading slug...");
    slug.upload(slugFile);
    logInfo("     - stack: " + slug.getStackName());
    logInfo("     - process types: " + processTypes);
  }

  protected void releaseSlug(Slug slug) throws IOException, Curl.CurlException {
    logInfo("---> Releasing...");
    Map releaseResponse = slug.release();
    logDebug("Heroku Release response: " + releaseResponse);
    logInfo("     - version: " + releaseResponse.get("version"));
  }

  protected String getJdkVersion() {
    String defaultJdkVersion = "1.8";
    File sysPropsFile = new File(rootDir, "system.properties");
    if (sysPropsFile.exists()) {
      Properties props = new Properties();
      try {
        props.load(new FileInputStream(sysPropsFile));
        return props.getProperty("java.runtime.version", defaultJdkVersion);
      } catch (IOException e) {
        logDebug(e.getMessage());
      }
    }
    return defaultJdkVersion;
  }

  protected Properties getHerokuProperties() {
    Properties props = new Properties();
    String defaultJdkVersion = "1.8";
    File sysPropsFile = new File(rootDir, "heroku.properties");
    if (sysPropsFile.exists()) {
      try {
        props.load(new FileInputStream(sysPropsFile));
      } catch (IOException e) {
        logDebug(e.getMessage());
      }
    }
    return props;
  }

  protected Map<String, String> getProcfile() {
    Map<String, String> procTypes = new HashMap<String, String>();

    File procfile = new File(rootDir, "Procfile");
    if (procfile.exists()) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(procfile));
        String line = reader.readLine();
        while (line != null) {
          Integer colon = line.indexOf(":");
          String key = line.substring(0, colon);
          String value = line.substring(colon + 1);
          procTypes.put(key.trim(), value.trim());

          line = reader.readLine();
        }
      } catch (Exception e) {
        logDebug(e.getMessage());
      }
    }

    return procTypes;
  }

  private void vendorJdk(String jdkVersion, URL jdkUrl, String stackName) throws IOException, InterruptedException, ArchiveException {
    URL realJdkUrl = jdkUrl;
    String realJdkVersion = "default";
    if (realJdkUrl == null) {
      realJdkVersion = jdkVersion == null ? getJdkVersion() : jdkVersion;
      if (jdkUrlsByStack.containsKey(stackName)) {
        Map<String, String> jdkUrlStrings = jdkUrlsByStack.get(stackName);
        if (jdkUrlStrings.containsKey(realJdkVersion)) {
          realJdkUrl = new URL(jdkUrlStrings.get(realJdkVersion));
        } else {
          throw new IllegalArgumentException("Invalid JDK version: " + realJdkVersion);
        }
      } else {
        throw new IllegalArgumentException("Unsupported Stack: " + stackName);
      }
      logInfo("     - installing: OpenJDK " + realJdkVersion);

      Files.write(
          Paths.get(new File(getAppDir(), "system.properties").getPath()),
          ("java.runtime.version=" + realJdkVersion).getBytes(StandardCharsets.UTF_8)
      );
    } else {
      logInfo("     - installing: Custom JDK");
    }

    File jdkHome = new File(getAppDir(), ".jdk");
    jdkHome.mkdir();

    String hashedString = "default";
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("MD5");
      messageDigest.update(realJdkUrl.toString().getBytes());
      hashedString = (new HexBinaryAdapter()).marshal((messageDigest.digest()));
    } catch (NoSuchAlgorithmException e) { /* no-op */ }

    File jdkTgz = new File(getHerokuDir(), "jdk-" + realJdkVersion + "-" + hashedString + ".tar.gz");
    if (!jdkTgz.exists()) {
      // TODO also check md5
      FileUtils.copyURLToFile(realJdkUrl, jdkTgz);
    }

    Tar.extract(jdkTgz, jdkHome);
  }

  private void addProfileScript() throws IOException {
    File profiledDir = new File(getAppDir(), ".profile.d");
    profiledDir.mkdir();

    Files.write(
        Paths.get(new File(profiledDir, "jvmcommon.sh").getPath()),
        ("" +
            "limit=$(ulimit -u)\n" +
            "case $limit in\n" +
            "256)   # 1X Dyno\n" +
            "  heap=384\n" +
            ";;\n" +
            "512)   # 2X Dyno\n" +
            "  heap=768\n" +
            ";;\n" +
            "32768) # PX Dyno\n" +
            "  heap=6144\n" +
            ";;\n" +
            "*)\n" +
            "  heap=384\n" +
            ";;\n" +
            "esac\n" +
            "export JAVA_TOOL_OPTIONS=\"-Xmx${heap}m $JAVA_TOOL_OPTIONS -Djava.rmi.server.useCodebaseOnly=true\"\n" +
            "export PATH=\"$HOME/.jdk/bin:$PATH\"" +
            "").getBytes(StandardCharsets.UTF_8)
    );
  }

  protected String relativize(File path) {
    return rootDir.toURI().relativize(path.toURI()).getPath();
  }

  protected String getEncodedApiKey() throws IOException {
    if (encodedApiKey == null) {
      String apiKey = System.getenv("HEROKU_API_KEY");
      if (null == apiKey || apiKey.isEmpty()) {
        try {
          apiKey = Toolbelt.getApiToken();
        } catch (Exception e) {
          // do nothing
        }
      }

      if (apiKey == null || apiKey.isEmpty()) {
        throw new RuntimeException("Could not get API key! Please install the toolbelt and login with `heroku login` or set the HEROKU_API_KEY environment variable.");
      }
      encodedApiKey = new BASE64Encoder().encode((":" + apiKey).getBytes());
    }
    return encodedApiKey;
  }

  protected File getAppDir() {
    return new File(getHerokuDir(), "app");
  }

  protected File getHerokuDir() {
    return new File(targetDir, "heroku");
  }

  protected File getRootDir() {
    return rootDir;
  }

  private static File createTempDir() throws IOException {
    return Files.createTempDirectory("heroku-deploy").toFile();
  }

  public static class CopyFileVisitor extends SimpleFileVisitor<Path> {
    private final Path targetPath;
    private Path sourcePath = null;

    public CopyFileVisitor(Path targetPath) {
      this.targetPath = targetPath;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
      if (dir.equals(targetPath)) {
        return FileVisitResult.SKIP_SUBTREE;
      } else if (sourcePath == null) {
        sourcePath = dir;
      }
      Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
      Files.copy(file, targetPath.resolve(sourcePath.relativize(file)), StandardCopyOption.COPY_ATTRIBUTES);
      return FileVisitResult.CONTINUE;
    }
  }
}
