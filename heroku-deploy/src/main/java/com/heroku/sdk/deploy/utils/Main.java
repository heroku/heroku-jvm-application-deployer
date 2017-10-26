package com.heroku.sdk.deploy.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.heroku.sdk.deploy.App;

/**
 * @author Joe Kutner on 10/24/17.
 *         Twitter: @codefinger
 */
public class Main {

  @FunctionalInterface
  public interface DeployFunction<T, B, R> {
    R apply(T t, B b) throws IOException;
  }

  private static List<File> includesToFiles(String includes) {
    List<String> includeStrings = includesToList(includes, File.pathSeparator);

    List<File> includeFiles = new ArrayList<>(includeStrings.size());
    for (String includeString : includeStrings) {
      if (!includeString.isEmpty()) {
        includeFiles.add(new File(includeString));
      }
    }

    return includeFiles;
  }

  private static List<String> includesToList(String includes, String delim) {
    return includes == null || includes.isEmpty() ?
        new ArrayList<>() :
        Arrays.asList(includes.split(delim));
  }

  public static void deploy(DeployFunction<String, List<String>, App> f) throws Exception {
    String appName = System.getProperty("heroku.appName", null);
    String jdkVersion = System.getProperty("heroku.jdkVersion", null);
    List<File> includes = includesToFiles(System.getProperty("heroku.includes", ""));
    String buildFileName = System.getProperty("heroku.buildFileName", "slug.tgz");
    String buildpacksDelim = System.getProperty("heroku.buildpacksDelim", ",");
    List<String> buildpacks = includesToList(System.getProperty("heroku.buildpacks", ""), buildpacksDelim);

    if (appName == null) {
      throw new IllegalArgumentException("Heroku app name must be provided with heroku.appName system property!");
    }

    f.apply(appName, buildpacks).
        deploy(includes, new HashMap<>(), jdkVersion, buildFileName);
  }

  public static Boolean isDebug() {
    String debug = System.getenv("HEROKU_DEBUG");
    return "1".equals(debug) || "true".equals(debug);
  }
}
