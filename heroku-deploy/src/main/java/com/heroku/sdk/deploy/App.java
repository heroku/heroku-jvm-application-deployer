package com.heroku.sdk.deploy;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FileUtils;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

public class App {

  private static Map<String,Map<String,String>> jdkUrlsByStack = new HashMap<String,Map<String,String>>();

  static {
    Map<String,String> cedarJdkUrlStrings = new HashMap<String,String>();
    cedarJdkUrlStrings.put("1.6", "https://lang-jvm.s3.amazonaws.com/jdk/cedar/openjdk1.6-latest.tar.gz");
    cedarJdkUrlStrings.put("1.7", "https://lang-jvm.s3.amazonaws.com/jdk/cedar/openjdk1.7-latest.tar.gz");
    cedarJdkUrlStrings.put("1.8", "https://lang-jvm.s3.amazonaws.com/jdk/cedar/openjdk1.8-latest.tar.gz");

    Map<String,String> cedar14JdkUrlStrings = new HashMap<String,String>();
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
    this.name = name;
    this.rootDir = rootDir;
    this.targetDir = targetDir;

    try {
      FileUtils.forceDelete(getHerokuDir());
    } catch (IOException e) {
      // do nothing
    }

    getHerokuDir().mkdir();
    getAppDir().mkdir();
  }

  protected void deploy(List<File> includedFiles, Map<String,String> configVars, String jdkVersion, URL jdkUrl, Map<String,String> processTypes) throws Exception {
    prepare(includedFiles);

    Map<String,String> existingConfigVars = getConfigVars();
    logDebug("Heroku existing config variables: " + existingConfigVars.keySet());

    Map<String,String> newConfigVars = new HashMap<String, String>();
    newConfigVars.putAll(addConfigVar("PATH", ".jdk/bin:/usr/local/bin:/usr/bin:/bin", existingConfigVars, true));
    for (String key : configVars.keySet()) {
      newConfigVars.putAll(addConfigVar(key, configVars.get(key), existingConfigVars));
    }
    setConfigVars(newConfigVars);

    deploySlug(jdkVersion, jdkUrl, processTypes);
  }

  public void deploy(List<File> includedFiles, Map<String,String> configVars, String jdkVersion, Map<String,String> processTypes) throws Exception {
    deploy(includedFiles, configVars, jdkVersion, null, processTypes);
  }

  public void deploy(List<File> includedFiles, Map<String,String> configVars, URL jdkUrl, Map<String,String> processTypes) throws Exception {
    deploy(includedFiles, configVars, jdkUrl.toString(), jdkUrl, processTypes);
  }

  protected void prepare(List<File> includedFiles) throws Exception {
    logInfo("---> Packaging application...");
    logInfo("     - app: " + name);

    try {
      for (File file : includedFiles) {
        logInfo("     - including: ./" + relativize(file));
        copy(file, new File(getAppDir(), relativize(file)));

        addProfileScript();
      }
    } catch (IOException ioe) {
      throw new Exception("There was an error packaging the application for deployment.", ioe);
    }
  }

  protected void copy(File file, File copyTarget) throws IOException {
    if (SystemSettings.hasNio()) {
      if (file.isDirectory()) {
        Files.walkFileTree(file.toPath(), new CopyFileVisitor(copyTarget.toPath()));
      } else {
        Files.createDirectories(copyTarget.getParentFile().toPath());
        Files.copy(file.toPath(), copyTarget.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
      }
    } else {
      if (file.isDirectory()) {
        FileUtils.copyDirectory(file, new File(getAppDir(), relativize(file)));
      } else {
        FileUtils.copyFile(file, new File(getAppDir(), relativize(file)));
      }
    }
  }

  public Map<String,String> getConfigVars() throws Exception {
    String urlStr = Slug.BASE_URL + "/apps/" + URLEncoder.encode(name, "UTF-8") + "/config-vars";

    Map<String,String> headers = new HashMap<String,String>();
    headers.put("Authorization", getEncodedApiKey());
    headers.put("Accept", "application/vnd.heroku+json; version=3");

    Map m = Curl.get(urlStr, headers);
    Map<String,String> configVars = new HashMap<String,String>();
    for (Object key : m.keySet()) {
      Object value = m.get(key);
      if ((key instanceof String) && (value instanceof String)) {
        configVars.put(key.toString(), value.toString());
      } else {
        throw new Exception("Unexpected return type: " + m);
      }
    }
    return configVars;
  }

  protected void setConfigVars(Map<String,String> configVars) throws IOException, Curl.CurlException {
    if (!configVars.isEmpty()) {
      String urlStr = Slug.BASE_URL + "/apps/" + URLEncoder.encode(name, "UTF-8") + "/config_vars";

      String data = "{";
      boolean first = true;
      for (String key : configVars.keySet()) {
        String value = configVars.get(key);
        if (!first) data += ", ";
        first = false;
        data += "\"" + key + "\"" + ":" + "\"" + sanitizeJson(value) + "\"";
      }
      data +=  "}";

      Map<String,String> headers = new HashMap<String,String>();
      headers.put("Authorization", getEncodedApiKey());
      headers.put("Accept", "application/json");

      Curl.put(urlStr, data, headers);
    }
  }

  protected Slug deploySlug(String jdkVersion, URL jdkUrl, Map<String,String> processTypes) throws IOException, Curl.CurlException, ArchiveException, InterruptedException {
    Map<String,String> allProcessTypes = getProcfile();
    allProcessTypes.putAll(processTypes);
    if (allProcessTypes.isEmpty()) logWarn("No processTypes specified!");

    Slug slug = new Slug(buildPackDesc, name, getEncodedApiKey(), allProcessTypes);
    logDebug("Heroku Slug request: " + slug.getSlugRequest());

    Map slugResponse = slug.create();
    logDebug("Heroku Slug response: " + slugResponse);
    logDebug("Heroku Blob URL: " + slug.getBlobUrl());
    logDebug("Heroku Slug Id: " + slug.getSlugId());

    vendorJdk(jdkVersion, jdkUrl, slug.getStackName());

    logInfo("---> Creating slug...");
    File slugFile = Tar.create("slug", "./app", getHerokuDir());
    logInfo("     - file: ./" + relativize(slugFile));
    logInfo("     - size: " + (slugFile.length() / (1024 * 1024)) + "MB");

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

  protected Map<String,String> getProcfile() {
    Map<String,String> procTypes = new HashMap<String, String>();

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
    if (realJdkUrl == null) {
      String realJdkVersion = jdkVersion == null ? getJdkVersion() : jdkVersion;
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

    File jdkTgz = new File(getHerokuDir(), "jdk-pkg.tar.gz");
    FileUtils.copyURLToFile(realJdkUrl, jdkTgz);

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
        "").getBytes(StandardCharsets.UTF_8)
    );
  }

  protected String relativize(File path) {
    return rootDir.toURI().relativize(path.toURI()).getPath();
  }

  protected String getEncodedApiKey() throws IOException {
    if (encodedApiKey == null) {
      String apiKey = System.getenv("HEROKU_API_KEY");
      if (null == apiKey || apiKey.equals("")) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<String> future =
            new FutureTask<String>(new Callable<String>() {
              public String call() throws IOException {
                String herokuCmd = SystemSettings.isWindows() ? "heroku.bat" : "heroku";
                ProcessBuilder pb = new ProcessBuilder().command(herokuCmd, "auth:token");
                Process p = pb.start();

                BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                String output = "";
                while ((line = bri.readLine()) != null) {
                  output += line;
                }
                return output;
              }});

        executor.execute(future);

        try {
          apiKey = future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
          throw new RuntimeException("Could not get API key! Please login with `heroku auth:login` or set the HEROKU_API_KEY environment variable.");
        }
      }
      encodedApiKey = new BASE64Encoder().encode((":" + apiKey).getBytes());
    }
    return encodedApiKey;
  }

  private Map<String,String> addConfigVar(String key, String value, Map<String,String> existingConfigVars) {
    return addConfigVar(key, value, existingConfigVars, false);
  }

  private Map<String,String> addConfigVar(String key, String value, Map<String,String> existingConfigVars, Boolean force) {
    Map<String,String> m = new HashMap<String,String>();
    if (!existingConfigVars.containsKey(key) || (!value.equals(existingConfigVars.get(key)) && force)) {
      m.put(key, value);
    }
    return m;
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

  protected String sanitizeJson(String json) {
    return json.replace("\\", "\\\\").replace("\"", "\\\"");
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
