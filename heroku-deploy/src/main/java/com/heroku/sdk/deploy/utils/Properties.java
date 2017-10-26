package com.heroku.sdk.deploy.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Joe Kutner on 10/20/17.
 *         Twitter: @codefinger
 */
public class Properties {

  static final java.util.Properties properties = new java.util.Properties();

  static {
    try {
      InputStream jarProps = Properties.class.getResourceAsStream("/heroku-deploy.properties");
      properties.load(jarProps);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getProperty(String propName) {
    return properties.getProperty(propName);
  }

  public static java.util.Properties getProperties() {
    return properties;
  }
}

