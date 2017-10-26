package com.heroku.sdk.deploy;

import com.heroku.api.HerokuAPI;
import org.junit.After;
import org.junit.Before;

/**
 * @author Joe Kutner on 10/26/17.
 *         Twitter: @codefinger
 */
public abstract class BaseDeployTest {

  protected HerokuAPI api;
  protected String appName;

  @Before
  public void setup() {
    this.api = new HerokuAPI(getApiKey());
    this.appName = api.createApp().getName();
    System.setProperty("heroku.appName", this.appName);
  }

  @After
  public void cleanupAppAndSysProps() {
    api.destroyApp(this.appName);
    System.clearProperty("heroku.appName");
    System.clearProperty("heroku.jdkVersion");
    System.clearProperty("heroku.includes");
    System.clearProperty("heroku.buildFileName");
    System.clearProperty("heroku.buildpacksDelim");
    System.clearProperty("heroku.buildpacks");
  }

  protected String getApiKey() {
    String key = System.getenv("HEROKU_API_KEY");
    if (null == key || key.isEmpty()) {
      try {
        key = Toolbelt.getApiToken();
      } catch (Exception e) {
        // do nothing
      }
    }

    if (key == null || key.isEmpty()) {
      throw new RuntimeException("Could not get API key! Please install the CLI and login with `heroku login` " +
          "or set the HEROKU_API_KEY environment variable.");
    }
    return key;
  }
}
