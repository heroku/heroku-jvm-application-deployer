package com.heroku.sdk.maven.mojo;

import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.lib.deploymemt.Deployer;
import com.heroku.sdk.deploy.lib.deploymemt.DeploymentDescriptor;
import com.heroku.sdk.deploy.lib.resolver.ApiKeyResolver;
import com.heroku.sdk.deploy.lib.resolver.AppNameResolver;
import com.heroku.sdk.deploy.lib.resolver.WebappRunnerResolver;
import com.heroku.sdk.deploy.lib.sourceblob.JvmProjectSourceBlobCreator;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobDescriptor;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobPackager;
import com.heroku.sdk.deploy.util.FileDownloader;
import com.heroku.sdk.deploy.util.PathUtils;
import com.heroku.sdk.deploy.util.Procfile;
import com.heroku.sdk.deploy.util.PropertiesUtils;
import com.heroku.sdk.maven.MavenLogOutputAdapter;
import com.heroku.sdk.maven.MojoExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class AbstractHerokuDeployMojo extends AbstractHerokuMojo {

    protected final void deploy(Mode mode) throws MojoExecutionException, MojoFailureException {
        try {
            OutputAdapter outputAdapter = new MavenLogOutputAdapter(getLog(), logProgress);

            Path projectDirectory = super.mavenProject.getBasedir().toPath();

            Path dependencyList = MojoExecutor.createDependencyListFile(super.mavenProject, super.mavenSession, super.pluginManager);

            ArrayList<Path> includedPaths = new ArrayList<>();

            for (String includePattern : super.includes) {
                includedPaths.addAll(resolveIncludePattern(includePattern));
            }

            includedPaths.add(Paths.get("pom.xml"));

            if (super.includeTarget) {
                includedPaths.add(Paths.get("target"));
            }

            Optional<Path> warFilePath = resolveWarFilePath(mode, projectDirectory);

            Supplier<Procfile> customProcfileResolver = () -> new Procfile(super.processTypes);
            if (mode == Mode.WAR) {
                if (!super.processTypes.isEmpty()) {
                    outputAdapter.logWarn("The processTypes property will be ignored when deploying a WAR file. Use `heroku:deploy` goal for custom processes.");
                }

                customProcfileResolver = () ->
                    Procfile.singleton("web", "java $JAVA_OPTS -jar webapp-runner.jar $WEBAPP_RUNNER_OPTS --port $PORT " + warFilePath.map(PathUtils::separatorsToUnix).get().toString());
            }

            SourceBlobDescriptor sourceBlobDescriptor = JvmProjectSourceBlobCreator.create(
                    projectDirectory,
                    "heroku-maven-plugin",
                    includedPaths,
                    customProcfileResolver,
                    Procfile.empty(),
                    () -> Optional.ofNullable(super.jdkVersion),
                    outputAdapter
            );

            sourceBlobDescriptor.addLocalPath("target/mvn-dependency-list.log", dependencyList, true);

            if (mode == Mode.WAR) {
                Path webappRunnerPath = FileDownloader.download(WebappRunnerResolver.getUrlForVersion(super.webappRunnerVersion));
                sourceBlobDescriptor.addLocalPath("webapp-runner.jar", webappRunnerPath, false);
            }

            Path sourceBlob = SourceBlobPackager.pack(sourceBlobDescriptor, outputAdapter);

            String appName = AppNameResolver.resolve(projectDirectory, () -> Optional.ofNullable(super.appName))
                    .orElseThrow(() -> new MojoExecutionException("Could not determine app name, please configure it explicitly!"));

            DeploymentDescriptor deploymentDescriptor
                    = new DeploymentDescriptor(appName, super.buildpacks, super.configVars, sourceBlob, mavenProject.getVersion());

            String apiKey = ApiKeyResolver
                    .resolve(projectDirectory)
                    .orElseThrow(() -> new MojoExecutionException("Could not resolve API key."));

            Properties pomProperties = PropertiesUtils.loadPomPropertiesOrEmptyFromClasspath(AbstractHerokuDeployMojo.class, "com.heroku.sdk", "heroku-deploy-standalone");

            Deployer.deploy(
                    apiKey,
                    "heroku-maven-plugin",
                    pomProperties.getProperty("version", "unknown"),
                    deploymentDescriptor,
                    outputAdapter);

        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Unexpected error!", e);
        }
    }

    private List<Path> resolveIncludePattern(String includePattern) {
        if (includePattern.contains("*")) {
            String[] dirs = includePattern.split(File.separator);
            String pattern = dirs[dirs.length - 1];
            File basedir = new File(mavenProject.getBasedir(), includePattern.replace(pattern, ""));

            return FileUtils
                    .listFiles(basedir, new WildcardFileFilter(pattern), null)
                    .stream()
                    .map(File::toPath)
                    .collect(Collectors.toList());
        } else {
            return Collections.singletonList(mavenProject.getBasedir().toPath().resolve(includePattern));
        }
    }

    private Optional<Path> resolveWarFilePath(Mode mode, Path projectDirectory) throws MojoExecutionException, IOException {
        if (mode == Mode.WAR) {
            return super.findWarFilePath(projectDirectory);
        }

        return Optional.empty();
    }

    protected enum Mode {
        GENERIC,
        WAR
    }
}
