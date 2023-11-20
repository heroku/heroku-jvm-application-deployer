package com.heroku.sdk.deploy.lib;

import java.util.Arrays;
import java.util.stream.Collectors;

public interface OutputAdapter {
    void logInfo(String message);
    void logDebug(String message);
    void logWarn(String message);
    void logError(String message);
    void logUploadProgress(long uploaded, long contentLength);

    default void logError(String message, Throwable t) {
        logError(message);
        logDebug(Arrays.stream(t.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n")));
    }
}
