package com.heroku.sdk.deploy.lib.deploy;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class DeploymentDescriptor {
    private String appName;
    private Optional<String> version;
    private List<String> buildpacks;
    private Path slug;

    public DeploymentDescriptor(String appName, List<String> buildpacks, Path slug, Optional<String> version) {
        this.appName = appName;
        this.version = version;
        this.buildpacks = buildpacks;
        this.slug = slug;
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

    public Path getSlug() {
        return slug;
    }
}
