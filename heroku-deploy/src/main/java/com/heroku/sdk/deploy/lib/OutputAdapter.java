package com.heroku.sdk.deploy.lib;

public interface OutputAdapter {
    void logInfo(String message);
    void logDebug(String message);
    void logWarn(String message);
    void logError(String message);

    void logUploadProgress(long uploaded, long contentLength);
}
