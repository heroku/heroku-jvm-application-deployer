package com.heroku.sdk.deploy.utils;

public interface UploadListener {
  public void logUploadProgress(Long uploaded, Long contentLength);
  public Boolean isEnabled();
}
