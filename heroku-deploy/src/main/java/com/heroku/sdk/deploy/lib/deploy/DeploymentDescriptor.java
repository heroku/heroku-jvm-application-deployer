package com.heroku.sdk.deploy.lib.deploy;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DeploymentDescriptor {
    private String appName;
    private Optional<String> version;
    private List<String> buildpacks;
    private Map<String, String> configVars;
    private Path sourceBlobPath;

    public DeploymentDescriptor(String appName, List<String> buildpacks, Map<String, String> configVars, Path sourceBlobPath, Optional<String> version) {
        this.appName = appName;
        this.version = version;
        this.buildpacks = buildpacks;
        this.configVars = configVars;
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

    public Map<String, String> getConfigVars() {
        return configVars;
    }

    public Path getSourceBlobPath() {
        return sourceBlobPath;
    }
}
