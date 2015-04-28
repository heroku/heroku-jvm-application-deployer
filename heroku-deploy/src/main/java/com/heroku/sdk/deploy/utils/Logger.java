package com.heroku.sdk.deploy.utils;

public interface Logger {

  public void logInfo(String message);

  public void logDebug(String message);

  public void logWarn(String message);

  public void logUploadProgress(Long uploaded, Long contentLength);

  public Boolean isUploadProgressEnabled();
}
