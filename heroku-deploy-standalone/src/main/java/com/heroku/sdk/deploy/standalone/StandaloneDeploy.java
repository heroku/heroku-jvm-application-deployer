package com.heroku.sdk.deploy.standalone;

import com.heroku.sdk.deploy.Constants;
import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.lib.deploymemt.Deployer;
import com.heroku.sdk.deploy.lib.deploymemt.DeploymentDescriptor;
import com.heroku.sdk.deploy.lib.resolver.ApiKeyResolver;
import com.heroku.sdk.deploy.lib.resolver.AppNameResolver;
import com.heroku.sdk.deploy.lib.resolver.WebappRunnerResolver;
import com.heroku.sdk.deploy.lib.sourceblob.JvmProjectSourceBlobCreator;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobDescriptor;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobPackager;
import com.heroku.sdk.deploy.util.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class StandaloneDeploy {
    private static OutputAdapter outputAdapter = new StdOutOutputAdapter(true);

    public static void deploy(Mode mode) throws IOException, InterruptedException {
        final Path projectDirectory = Paths.get(System.getProperty("user.dir"));


        final Optional<String> appName = AppNameResolver.resolve(projectDirectory, Optional::empty);
        if (!appName.isPresent()) {
            outputAdapter.logError("Heroku app name must be provided.");
            System.exit(-1);
        }

        Optional<String> apiKey = ApiKeyResolver.resolve(projectDirectory);
        if (!apiKey.isPresent()) {
            outputAdapter.logError("Heroku API key must be provided.");
            System.exit(-1);
        }

        Procfile defaultProcfile = Procfile.empty();
        List<Path> includedPaths = getIncludedPathsFromProperties();

        switch (mode) {
            case JAR:
                final String herokuJarFileSystemProperty = System.getProperty("heroku.jarFile");
                final String herokuJarOptsSystemProperty = System.getProperty("heroku.jarOpts", "");

                if (herokuJarFileSystemProperty == null) {
                    outputAdapter.logError("Path to existing JAR file must be provided with heroku.jarFile system property!");
                    System.exit(-1);
                }

                final Path localJarFilePath = Paths.get(herokuJarFileSystemProperty);
                includedPaths.add(localJarFilePath);

                String jarCommand = String.format(
                        "java $JAVA_OPTS -jar %s %s $JAR_OPTS",
                        // We fall back to an empty string if the path cannot be normalized. This will result in an
                        // user-readable error from JvmProjectSourceBlobCreator and the Procfile will never be deployed.
                        PathUtils.normalize(projectDirectory, localJarFilePath)
                                .map(PathUtils::separatorsToUnix)
                                .orElse(""),
                        herokuJarOptsSystemProperty);

                defaultProcfile = Procfile.singleton("web", jarCommand);
                break;
            case WAR:
                final String herokuWarFileSystemProperty = System.getProperty("heroku.warFile");

                if (herokuWarFileSystemProperty == null) {
                    outputAdapter.logError("Path to existing WAR file must be provided with heroku.warFile system property!");
                    System.exit(-1);
                }

                final Path localWarFilePath = Paths.get(herokuWarFileSystemProperty);
                includedPaths.add(localWarFilePath);

                String warCommand = String.format(
                        "java $JAVA_OPTS -jar webapp-runner.jar $WEBAPP_RUNNER_OPTS --port $PORT ./ %s",
                        // We fall back to an empty string if the path cannot be normalized. This will result in an
                        // user-readable error from JvmProjectSourceBlobCreator and the Procfile will never be deployed.
                        PathUtils.normalize(projectDirectory, localWarFilePath)
                                .map(PathUtils::separatorsToUnix)
                                .orElse(""));

                defaultProcfile = Procfile.singleton("web", warCommand);
                break;
        }

        SourceBlobDescriptor sourceBlobDescriptor = JvmProjectSourceBlobCreator.create(
                projectDirectory,
                "heroku-deploy-standalone",
                includedPaths,
                Procfile::empty,
                defaultProcfile,
                Optional::empty,
                outputAdapter
        );

        if (mode == Mode.WAR) {
            URI webappRunnerUri = Optional
                .ofNullable(System.getProperty("heroku.webappRunnerUrl", null))
                .map(URI::create)
                .orElseGet(() -> {
                    String version = System.getProperty("heroku.webappRunnerVersion", Constants.DEFAULT_WEBAPP_RUNNER_VERSION);
                    return WebappRunnerResolver.getUrlForVersion(version);
                });

            Path webappRunnerLocalPath = FileDownloader.download(webappRunnerUri);
            sourceBlobDescriptor.addLocalPath("webapp-runner.jar", webappRunnerLocalPath, true);
        }

        Path sourceBlobArchive = SourceBlobPackager.pack(sourceBlobDescriptor, outputAdapter);

        DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor(
                appName.get(),
                getBuildpacksFromProperties(),
                Collections.emptyMap(),
                sourceBlobArchive,
                GitUtils.getHeadCommitHash(projectDirectory).orElse("unknown"));

        Properties pomProperties = PropertiesUtils.loadPomPropertiesOrEmptyFromClasspath(StandaloneDeploy.class, "com.heroku.sdk", "heroku-deploy-standalone");

        Deployer.deploy(
                apiKey.get(),
                "heroku-deploy-standalone",
                pomProperties.getProperty("version", "unknown"),
                deploymentDescriptor,
                outputAdapter);
    }

    private static List<Path> getIncludedPathsFromProperties() {
        final String herokuIncludes = System.getProperty("heroku.includes");

        if (herokuIncludes == null) {
            return new ArrayList<>();
        }

        List<Path> includedPaths = new ArrayList<>();
        for (String filePathString : herokuIncludes.split(File.pathSeparator)) {
            includedPaths.add(Paths.get(filePathString));
        }

        return includedPaths;
    }

    private static List<String> getBuildpacksFromProperties() {
        String buildpacksString = System.getProperty("heroku.buildpacks");

        if (buildpacksString == null) {
            return Collections.emptyList();
        }

        String buildpacksDelim = System.getProperty("heroku.buildpacksDelim", ",");
        return Arrays.asList(buildpacksString.split(Pattern.quote(buildpacksDelim)));
    }

    public enum Mode {
        JAR, WAR
    }
}
