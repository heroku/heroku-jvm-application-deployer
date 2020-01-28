package com.heroku.sdk.maven;

import com.heroku.sdk.deploy.lib.OutputAdapter;
import org.apache.maven.plugin.logging.Log;

public class MavenLogOutputAdapter implements OutputAdapter {
    private final Log log;
    private boolean logUploadProgress;

    public MavenLogOutputAdapter(Log log, boolean logUploadProgress) {
        this.log = log;
        this.logUploadProgress = logUploadProgress;
    }

    @Override
    public void logInfo(String message) {
        log.info(message);
    }

    @Override
    public void logDebug(String message) {
        log.debug(message);
    }

    @Override
    public void logWarn(String message) {
        log.warn(message);
    }

    @Override
    public void logError(String message) {
        log.error(message);
    }

    @Override
    public void logUploadProgress(long uploaded, long contentLength) {
        if (logUploadProgress) {
            log.debug("[" + uploaded + "/" + contentLength + "]");
        }
    }
}
