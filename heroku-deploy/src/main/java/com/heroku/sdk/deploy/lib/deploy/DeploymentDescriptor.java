package com.heroku.sdk.deploy.lib.deploy;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class DeploymentDescriptor {
    private String appName;
    private Optional<String> version;
    private List<String> buildpacks;
    private Path sourceBlobPath;

    public DeploymentDescriptor(String appName, List<String> buildpacks, Path sourceBlobPath, Optional<String> version) {
        this.appName = appName;
        this.version = version;
        this.buildpacks = buildpacks;
        this.sourceBlobPath = sourceBlobPath;
    }

    public String getAppName() {
        return appName;
    }

    public Optional<String> getVersion() {
        return version;
    }

    public List<String> getBuildpacks() {
        return buildpacks;
    }

    public Path getSourceBlobPath() {
        return sourceBlobPath;
    }
}
