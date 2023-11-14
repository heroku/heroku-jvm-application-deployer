package com.heroku.sdk.deploy.lib.sourceblob;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SourceBlobDescriptor {
    private Map<Path, SourceBlobContent> contents = new HashMap<>();

    public SourceBlobDescriptor() {
    }

    private SourceBlobDescriptor(Map<Path, SourceBlobContent> contents) {
        this.contents = contents;
    }

    public boolean containsPath(Path sourceBlobPath) {
        return contents.keySet().contains(sourceBlobPath);
    }

    public void addLocalPath(Path sourceBlobPath, Path localPath, boolean hidden) {
        contents.put(sourceBlobPath, SourceBlobContent.fromLocalPath(localPath, hidden));
    }

    public void addLocalPath(String sourceBlobPath, Path localPath, boolean hidden) {
        addLocalPath(Paths.get(sourceBlobPath), localPath, hidden);
    }

    public void addSyntheticFile(Path sourceBlobPath, String stringContent, boolean hidden) {
        contents.put(sourceBlobPath, SourceBlobContent.fromString(stringContent, hidden));
    }

    public void addSyntheticFile(String sourceBlobPath, String stringContent, boolean hidden) {
        addSyntheticFile(Paths.get(sourceBlobPath), stringContent, hidden);
    }

    public Map<Path, SourceBlobContent> getContents() {
        return new HashMap<>(contents);
    }

    static class SourceBlobContent {
        private Path localPath;
        private String syntheticFileContents;
        private boolean hidden = false;

        public static SourceBlobContent fromLocalPath(Path localPath, boolean hidden) {
            SourceBlobContent sourceBlobContent = new SourceBlobContent();
            sourceBlobContent.localPath = localPath;
            sourceBlobContent.hidden = hidden;
            return sourceBlobContent;
        }

        public static SourceBlobContent fromString(String syntheticFileContents, boolean hidden) {
            SourceBlobContent sourceBlobContent = new SourceBlobContent();
            sourceBlobContent.syntheticFileContents = syntheticFileContents;
            sourceBlobContent.hidden = hidden;
            return sourceBlobContent;
        }

        public boolean isLocalPath() {
            return localPath != null;
        }

        public boolean isSyntheticFile() {
            return syntheticFileContents != null;
        }

        public Path getLocalPath() {
            return localPath;
        }

        public String getSyntheticFileContents() {
            return syntheticFileContents;
        }

        public boolean isHidden() {
            return hidden;
        }
    }
}
