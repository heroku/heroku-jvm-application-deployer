package com.heroku.sdk.deploy.standalone;

import com.heroku.sdk.deploy.api.ApiKeyResolver;
import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.lib.deploy.Deployer;
import com.heroku.sdk.deploy.lib.deploy.DeploymentDescriptor;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobContent;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobPackager;
import com.heroku.sdk.deploy.util.Procfile;
import com.heroku.sdk.deploy.util.Util;
import com.heroku.sdk.deploy.util.WebappRunnerResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class StandaloneDeploy {
    private static OutputAdapter outputAdapter = new StdOutOutputAdapter(true);

    public static void deploy(Mode mode) throws IOException, InterruptedException {
        final Path applicationDirectory = Paths.get(System.getProperty("user.dir"));

        final String appName = System.getProperty("heroku.appName", "");
        final String jdkVersion = System.getProperty("heroku.jdkVersion", "1.8");

        final List<SourceBlobContent> includedPaths = getIncludedPathsFromProperties(applicationDirectory);
        final List<String> buildpacks = getBuildpacksFromProperties(); // TODO: Resolving? See 2.x.

        final Procfile procfile = Procfile.fromFile(applicationDirectory.resolve("Procfile"));

        if (appName.isEmpty()) {
            System.err.println("Heroku app name must be provided with heroku.appName system property!");
            System.exit(-1);
        }

        Optional<String> optionalApiKey = ApiKeyResolver.resolve(applicationDirectory);
        if (!optionalApiKey.isPresent()) {
            System.err.println("Could not get API key! Please install the Heroku CLI and run `heroku login` or set the HEROKU_API_KEY environment variable.");
            System.exit(-1);
        }

        switch(mode) {
            case JAR:
                final Path jarFile = Paths.get(System.getProperty("heroku.jarFile", "")).toAbsolutePath();
                final String jarOpts = System.getProperty("heroku.jarOpts", "");

                if (!Files.exists(jarFile)) {
                    System.err.println("Path to existing JAR file must be provided with heroku.jarFile system property!");
                    System.exit(-1);
                }

                Path slugJarFilePath = applicationDirectory.relativize(jarFile);

                includedPaths.add(new SourceBlobContent(jarFile, slugJarFilePath)); // TODO: Check for outside of application dir?

                if (procfile.isEmpty()) {
                    procfile.add("web", "java $JAVA_OPTS -jar " + slugJarFilePath.toString() + " " + jarOpts + " $JAR_OPTS");
                }

                break;
            case WAR:
                final Path warFile = Paths.get(System.getProperty("heroku.warFile", "")).toAbsolutePath();
                Path slugWarFilePath = applicationDirectory.relativize(warFile);
                includedPaths.add(new SourceBlobContent(warFile, slugWarFilePath)); // TODO: Check for outside of application dir?

                final URI webappRunnerUri = getWebappRunnerUrl();

                Path webappRunnerPath = Files.createTempFile("heroku-deploy", "webapp-runner.jar");

                FileOutputStream fileOutputStream = new FileOutputStream(webappRunnerPath.toFile());
                ReadableByteChannel readableByteChannel = Channels.newChannel(webappRunnerUri.toURL().openStream());

                fileOutputStream.getChannel()
                        .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

                includedPaths.add(new SourceBlobContent(webappRunnerPath, Paths.get("webapp-runner.jar")));

                if (procfile.isEmpty()) {
                    procfile.add("web", "java $JAVA_OPTS -jar webapp-runner.jar ${WEBAPP_RUNNER_OPTS} --port $PORT ./" + slugWarFilePath.toString());
                }

                break;
        }

        includedPaths.add(new SourceBlobContent(Util.createTemporaryFileWithStringContents(procfile.asString()), Paths.get("Procfile")));
        includedPaths.add(new SourceBlobContent(Util.createTemporaryFileWithStringContents("client=heroku-deploy 3.x"), Paths.get(".heroku-deploy")));

        if (Files.isRegularFile(applicationDirectory.resolve("system.properties"))) {
            includedPaths.add(new SourceBlobContent(applicationDirectory.resolve("system.properties"), Paths.get("system.properties")));
        } else {
            includedPaths.add(new SourceBlobContent(Util.createTemporaryFileWithStringContents("java.runtime.version=" + jdkVersion), Paths.get("system.properties")));
        }

        Path slugArchive = SourceBlobPackager.pack(includedPaths, outputAdapter);
        DeploymentDescriptor deploymentDescriptor = new DeploymentDescriptor(appName, buildpacks, Collections.emptyMap(), slugArchive, Optional.of("1.0.0")); // TODO: Resolve version
        Deployer.deploy(optionalApiKey.get(), deploymentDescriptor, outputAdapter);
    }

    private static List<SourceBlobContent> getIncludedPathsFromProperties(Path applicationDirectory) {
        final String herokuIncludes = System.getProperty("heroku.includes", "");

        if (herokuIncludes.isEmpty()) {
            return new ArrayList<>();
        }

        List<SourceBlobContent> includedPaths = new ArrayList<>();
        for (String filePathString : herokuIncludes.split(File.pathSeparator)) {
            Path filePath = Paths.get(filePathString);
            Path relativeFilePath = applicationDirectory.relativize(filePath); // TODO: What about paths outside of applicationDirectory?

            includedPaths.add(new SourceBlobContent(filePath, relativeFilePath));
        }

        return includedPaths;
    }

    private static List<String> getBuildpacksFromProperties() {
        String buildpacksDelim = System.getProperty("heroku.buildpacksDelim", ","); // TODO: This will be treated as a regex!
        String buildpacksString = System.getProperty("heroku.buildpacks", "");

        if (buildpacksString.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.asList(buildpacksString.split(buildpacksDelim));
    }

    private static URI getWebappRunnerUrl() {
        String explicitWebappRunnerUrl = System.getProperty("heroku.webappRunnerUrl", null);
        if (explicitWebappRunnerUrl != null) {
            return URI.create(explicitWebappRunnerUrl);
        }

        final String webappRunnerVersion = System.getProperty("heroku.webappRunnerVersion", "9.0.30.0");
        return WebappRunnerResolver.getUrlForVersion(webappRunnerVersion);
    }

    public enum Mode {
        JAR, WAR
    }
}
