package com.heroku.sdk.deploy.endpoints;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.heroku.sdk.deploy.utils.Properties;
import org.eclipse.jgit.util.Base64;

public abstract class ApiEndpoint {
  public static final String BASE_URL = "https://api.heroku.com";

  private final String userAgentValuePattern = "heroku-deploy/%s (%s) Java/%s (%s)";

  protected String blobUrl;

  protected String appName;

  protected Boolean useCurl;

  protected Map<String,String> headers;

  ApiEndpoint(String appName, String client, String apiKey) throws IOException {
    this.appName = appName;
    this.useCurl = "true".equals(System.getProperty("heroku.curl.enabled", "false"));
    headers = new HashMap<>();
    headers.put("Authorization", encodeApiKey(apiKey));
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "application/vnd.heroku+json; version=3");
    headers.put("User-Agent", getUserAgentValue(client));
  }

  private String getUserAgentValue(String client) {
    return String.format(
        userAgentValuePattern,
        Properties.getProperty("heroku-deploy.version"),
        client,
        System.getProperty("java.version"),
        System.getProperty("java.vendor"));
  }

  private String encodeApiKey(String apiKey) throws IOException {
    return Base64.encodeBytes((":" + apiKey).getBytes());
  }
}
