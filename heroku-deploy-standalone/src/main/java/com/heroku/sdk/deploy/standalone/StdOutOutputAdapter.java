package com.heroku.sdk.deploy.standalone;

import com.heroku.sdk.deploy.lib.OutputAdapter;

public class StdOutOutputAdapter implements OutputAdapter {
    private boolean suppressUploadProgress;

    public StdOutOutputAdapter(boolean suppressUploadProgress) {
        this.suppressUploadProgress = suppressUploadProgress;
    }

    @Override
    public void logInfo(String message) {
        System.out.println("INFO: " + message);
    }

    @Override
    public void logDebug(String message) {
        System.out.println("DEBUG: " + message);
    }

    @Override
    public void logWarn(String message) {
        System.out.println("WARN: " + message);
    }

    @Override
    public void logError(String message) {
        System.out.println("ERROR: " + message);
    }

    @Override
    public void logUploadProgress(long uploaded, long contentLength) {
        if (!suppressUploadProgress) {
            System.out.printf("Upload progress: %.0f%%\n", ((double) uploaded / (double) contentLength) * 100);
        }
    }
}
