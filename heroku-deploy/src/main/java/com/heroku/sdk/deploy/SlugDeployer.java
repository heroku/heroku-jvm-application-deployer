package com.heroku.sdk.deploy;

import com.heroku.sdk.deploy.endpoints.Slug;
import com.heroku.sdk.deploy.utils.Logger;
import com.heroku.sdk.deploy.utils.Tar;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.io.FileUtils;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class SlugDeployer extends Deployer {

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

  public SlugDeployer(String buildPackDesc,String name, File rootDir, File targetDir, Logger logger) {
    super(buildPackDesc, name, rootDir, targetDir, logger);
  }

  protected void createSlug(String slugFilename, String jdkVersion, URL jdkUrl, String stack) throws Exception {
    vendorJdk(jdkVersion, jdkUrl, stack);
    buildSlugFile(slugFilename);
  }

  public void releaseSlug(String slugFilename, Map<String, String> processTypes, Map<String, String> configVars, String stack) throws Exception {
    mergeConfigVars(configVars);

    File slugFile = new File(getHerokuDir(), slugFilename);
    if (slugFile.exists()) {
      logInfo("-----> Using existing slug...");
      logInfo("       - file: " + relativize(slugFile));
      logInfo("       - size: " + (slugFile.length() / (1024 * 1024)) + "MB");
      deploySlug(stack, processTypes, slugFile);
    } else {
      throw new FileNotFoundException("Slug file not found!");
    }
  }

  protected File buildSlugFile(String slugFilename)
      throws InterruptedException, ArchiveException, IOException {
    logInfo("-----> Creating slug...");
    try {
      FileUtils.forceDelete(new File(getHerokuDir(), slugFilename));
    } catch (IOException e) { /* no-op */ }
    File slugFile = Tar.create(slugFilename, "./app", getHerokuDir(), getHerokuDir());
    logInfo("       - file: " + relativize(slugFile));
    logInfo("       - size: " + (slugFile.length() / (1024 * 1024)) + "MB");
    return slugFile;
  }

  protected void deploySlug(String stack, Map<String, String> processTypes, File slugFile)
      throws IOException, ArchiveException, InterruptedException {
    Map<String, String> allProcessTypes = getProcfile();
    allProcessTypes.putAll(processTypes);
    if (allProcessTypes.isEmpty()) logWarn("No processTypes specified!");

    Slug slug = new Slug(buildPackDesc, name, stack, parseCommit(), getEncodedApiKey(), allProcessTypes);
    logDebug("Heroku Slug request: " + slug.getSlugRequest());

    Map slugResponse = slug.create();
    logDebug("Heroku Slug response: " + slugResponse);
    logDebug("Heroku Blob URL: " + slug.getBlobUrl());
    logDebug("Heroku Slug Id: " + slug.getSlugId());

    uploadSlug(slug, slugFile, ((Map) slugResponse.get("process_types")).keySet());

    releaseSlug(slug);
  }

  @Override
  protected void addExtras(Map<String, String> processTypes) throws IOException {
    addProfileScript();
    addStartupFiles();
  }

  protected void vendorJdk(String jdkVersion, URL jdkUrl, String stackName) throws IOException, InterruptedException, ArchiveException {
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
      logInfo("       - installing: OpenJDK " + realJdkVersion);

      Files.write(
          Paths.get(new File(getAppDir(), "system.properties").getPath()),
          ("java.runtime.version=" + realJdkVersion).getBytes(StandardCharsets.UTF_8)
      );
    } else {
      logInfo("       - installing: Custom JDK");
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

    addJdkOverlay();
  }

  private void addProfileScript() throws IOException {
    File profiledDir = new File(getAppDir(), ".profile.d");
    profiledDir.mkdir();

    Files.write(
        Paths.get(new File(profiledDir, "jvmcommon.sh").getPath()),
        ("" +
            "export PATH=\"$HOME/.jdk/bin:$HOME/.startup:$PATH\"\n" +
            "export JAVA_HOME=\"\\$HOME/.jdk\"\n" +
            "limit=$(ulimit -u)\n" +
            "case $limit in\n" +
            "256)   # 1X Dyno\n" +
            "  default_java_opts=\"-Xmx384m -Xss512k\"\n" +
            ";;\n" +
            "512)   # 2X Dyno\n" +
            "  default_java_opts=\"-Xmx768m\"\n" +
            ";;\n" +
            "32768) # PX Dyno\n" +
            "  default_java_opts=\"-Xmx4g\"\n" +
            ";;\n" +
            "*)\n" +
            "  default_java_opts=\"-Xmx384m -Xss512k\"\n" +
            ";;\n" +
            "esac\n" +
            "export JAVA_TOOL_OPTIONS=\"${JAVA_TOOL_OPTIONS:-\"${default_java_opts} -Dfile.encoding=UTF-8 -Djava.rmi.server.useCodebaseOnly=true\"}\"\n" +
            "").getBytes(StandardCharsets.UTF_8)
    );
  }

  protected void addStartupFiles() throws IOException {
    File startupDir = new File(getAppDir(), ".startup");
    startupDir.mkdir();

    File withJmap = new File(startupDir, "with_jmap");
    copyResourceFile("heroku_with_jmap.sh", withJmap);
    withJmap.setExecutable(true);

    File withJstack = new File(startupDir, "with_jstack");
    copyResourceFile("heroku_with_jstack.sh", withJstack);
    withJstack.setExecutable(true);
  }

  protected void addJdkOverlay() throws IOException {
    File jdkDir = new File(getAppDir(), ".jdk");
    File jdkOverlayDir = new File(getRootDir(), ".jdk-overlay");

    if (jdkOverlayDir.exists() && jdkDir.exists()) {
      logInfo("       - applying JDK overlay");
      FileUtils.copyDirectory(jdkOverlayDir, jdkDir);
    }
  }
}
