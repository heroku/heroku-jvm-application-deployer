package com.heroku.sdk.deploy.lib.sourceblob;

import java.nio.file.Path;

public final class SourceBlobContent {
    private final Path localPath;
    private final Path sourceBlobPath;
    private boolean hidden;

    public SourceBlobContent(Path localPath, Path sourceBlobPath) {
        this.localPath = localPath;
        this.sourceBlobPath = sourceBlobPath;
    }

    public SourceBlobContent(Path localPath, Path sourceBlobPath, boolean hidden) {
        this.localPath = localPath;
        this.sourceBlobPath = sourceBlobPath;
        this.hidden = hidden;
    }

    public Path getLocalPath() {
        return localPath;
    }

    public Path getSourceBlobPath() {
        return sourceBlobPath;
    }

    public boolean isHidden() {
        return hidden;
    }

    @Override
    public String toString() {
        return "SourceBlobContent{" +
                "localPath=" + localPath +
                ", sourceBlobPath=" + sourceBlobPath +
                ", hidden=" + hidden +
                '}';
    }
}
