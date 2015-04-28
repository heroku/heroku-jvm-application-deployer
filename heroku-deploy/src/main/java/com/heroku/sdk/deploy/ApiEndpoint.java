package com.heroku.sdk.deploy;

import com.heroku.sdk.deploy.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class ApiEndpoint {
  public static final String BASE_URL = "https://api.heroku.com";

  protected String blobUrl;

  protected String stackName;

  protected String appName;

  protected String commit;

  protected Map<String,String> headers;

  public ApiEndpoint(String appName, String stackName, String commit, String encodedApiKey) {
    this.appName = appName;
    this.stackName = stackName;
    this.commit = commit;

    headers = new HashMap<String,String>();
    headers.put("Authorization", encodedApiKey);
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "application/vnd.heroku+json; version=3");
  }

  public void upload(File slugFile, Logger listener) throws IOException {
    if (blobUrl == null) {
      throw new IllegalStateException("Slug must be created before uploading!");
    }

    RestClient.put(blobUrl, slugFile, listener);
  }

  public String getBlobUrl() { return blobUrl; }
  public String getStackName() { return stackName; }
  public String getCommit() { return commit; }
}
