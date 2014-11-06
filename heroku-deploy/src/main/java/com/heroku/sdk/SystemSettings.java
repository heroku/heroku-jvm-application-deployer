package com.heroku.sdk;

public class SystemSettings {

  public static Boolean hasNio() {
    String ver = System.getProperty("java.specification.version");
    return "1.7".equals(ver) || "1.8".equals(ver);
  }
}
