package com.heroku.sdk.deploy;

import com.heroku.sdk.deploy.utils.Logger;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public abstract class Deployer {

  protected String buildPackDesc;

  protected String name;

  protected File rootDir;

  protected File targetDir;

  protected String encodedApiKey = null;

  protected Logger logger;

  public void logInfo(String message) { logger.logInfo(message); }

  public void logDebug(String message) { logger.logDebug(message); }

  public void logWarn(String message) { logger.logWarn(message); }

  public Deployer(String buildPackDesc, String name, File rootDir, File targetDir, Logger logger) {
    this.logger = logger;
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

  public String getBuildPackDesc() {
    return buildPackDesc;
  }

  protected void deploy(Map<String, String> configVars, String jdkVersion, URL jdkUrl, String stack, Map<String, String> processTypes, String slugFilename) throws Exception {
    mergeConfigVars(configVars);
    vendorJdk(jdkVersion, jdkUrl, stack);
    createAndReleaseSlug(stack, processTypes, slugFilename);
  }

  public void prepare(List<File> includedFiles, Map<String, String> processTypes) throws IOException {
    logInfo("---> Packaging application...");
    logInfo("     - app: " + name);

    try {
      for (File file : includedFiles) {
        logInfo("     - including: " + relativize(file));
        copy(file, new File(getAppDir(), relativize(file)));
      }
      try {
        // this makes sure we don't put an old slug or a cached jdk inside the slug
        FileUtils.forceDelete(new File(getAppDir(), relativize(getHerokuDir())));
      } catch (IOException e) { /* do nothing */ }
      addExtras(processTypes);
    } catch (IOException ioe) {
      throw new IOException("There was an error packaging the application for deployment.", ioe);
    }
  }

  protected void addExtras(Map<String, String> processTypes) throws IOException { /* nothing extra by default */ }

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

  protected void createAndReleaseSlug(String stack, Map<String, String> processTypes, String slugFilename)
      throws IOException, ArchiveException, InterruptedException {
    deploySlug(stack, processTypes, buildSlugFile(slugFilename));
    logInfo("---> Done");
  }

  protected abstract File buildSlugFile(String slugFilename)
      throws InterruptedException, ArchiveException, IOException;

  protected abstract void deploySlug(String stack, Map<String, String> processTypes, File slugFile)
      throws IOException, ArchiveException, InterruptedException;

  protected void uploadSlug(Slug slug, File slugFile, Set processTypes)
      throws IOException, ArchiveException, InterruptedException {
    logInfo("---> Uploading slug...");
    slug.upload(slugFile, logger);
    logInfo("     - stack: " + slug.getStackName());
    logInfo("     - process types: " + processTypes);
  }

  protected void releaseSlug(Slug slug) throws IOException {
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

  protected abstract void vendorJdk(String jdkVersion, URL jdkUrl, String stackName) throws IOException, InterruptedException, ArchiveException;

  protected String relativize(File path) {
    if (path.isAbsolute() && !path.getPath().startsWith(rootDir.getPath())) {
      return path.getName();
    } else {
      return rootDir.toURI().relativize(path.toURI()).getPath();
    }
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

  protected File getTargetDir() {
    return targetDir;
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

  protected void copyResourceFile(String srcFilename, File targetFile) throws IOException {
    BufferedWriter out = null;
    try {
      InputStream is = getClass().getResourceAsStream( "/" + srcFilename);
      BufferedReader br = new BufferedReader(new InputStreamReader(is));

      FileWriter fw = new FileWriter(targetFile);
      out = new BufferedWriter(fw);

      String line;
      while ((line = br.readLine()) != null) {
        out.write(line);
        out.write("\n");
      }
    } finally {
      if (null != out) out.close();
    }
  }

  protected String parseCommit() throws IOException {
    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    Repository repository = builder.setWorkTree(getRootDir())
        .readEnvironment() // scan environment GIT_* variables
        .findGitDir() // scan up the file system tree
        .build();

    ObjectId head = repository.resolve("HEAD");
    return head == null ? null : head.name();
  }
}
