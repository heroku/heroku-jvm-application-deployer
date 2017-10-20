package com.heroku.sdk.deploy.endpoints;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.heroku.sdk.deploy.utils.Curl;
import com.heroku.sdk.deploy.utils.HerokuDeployProperties;
import com.heroku.sdk.deploy.utils.Logger;
import com.heroku.sdk.deploy.utils.RestClient;

public abstract class ApiEndpoint {
  public static final String BASE_URL = "https://api.heroku.com";

  private final String userAgentValuePattern = "heroku-deploy/%s (%s) Java/%s (%s)";

  protected String blobUrl;

  protected String stackName;

  protected String appName;

  protected String commit;

  protected Boolean useCurl;

  protected Map<String,String> headers;

  public ApiEndpoint(String appName, String client, String stackName, String commit, String encodedApiKey) {
    this.appName = appName;
    this.stackName = stackName;
    this.commit = commit;
    this.useCurl = "true".equals(System.getProperty("heroku.curl.enabled", "false"));
    headers = new HashMap<String,String>();
    headers.put("Authorization", encodedApiKey);
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "application/vnd.heroku+json; version=3");
    headers.put("User-Agent", getUserAgentValue(client));
  }

  public void upload(File slugFile, Logger listener) throws IOException, InterruptedException {
    if (blobUrl == null) {
      throw new IllegalStateException("Slug must be created before uploading!");
    }

    if (useCurl) {
      listener.logDebug("Uploading with curl");
      Curl.put(blobUrl, slugFile);
    } else {
      RestClient.put(blobUrl, slugFile, listener);
    }
  }

  public String getBlobUrl() { return blobUrl; }
  public String getStackName() { return stackName; }
  public String getCommit() { return commit; }


  public String getUserAgentValue(String client) {
    return String.format(
        userAgentValuePattern,
        HerokuDeployProperties.getProperty("heroku-deploy.version"),
        client,
        System.getProperty("java.version"),
        System.getProperty("java.vendor"));
  }
}
