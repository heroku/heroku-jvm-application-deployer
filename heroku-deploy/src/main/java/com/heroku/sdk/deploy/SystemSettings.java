package com.heroku.sdk.deploy;

public class SystemSettings {

  public static Boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }
}
