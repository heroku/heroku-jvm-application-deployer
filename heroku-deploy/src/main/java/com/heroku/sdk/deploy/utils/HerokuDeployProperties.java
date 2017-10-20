package com.heroku.sdk.deploy.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Joe Kutner on 10/20/17.
 *         Twitter: @codefinger
 */
public class HerokuDeployProperties {

  static final Properties properties = new Properties();

  static {
    try {
      InputStream jarProps = HerokuDeployProperties.class.getResourceAsStream("/heroku-deploy.properties");
      properties.load(jarProps);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getProperty(String propName) {
    return properties.getProperty(propName);
  }

  public static Properties getProperties() {
    return properties;
  }
}

