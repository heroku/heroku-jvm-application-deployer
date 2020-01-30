package com.heroku.sdk.maven.mojo;

import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.lib.deploymemt.Deployer;
import com.heroku.sdk.deploy.lib.deploymemt.DeploymentDescriptor;
import com.heroku.sdk.deploy.lib.resolver.ApiKeyResolver;
import com.heroku.sdk.deploy.lib.resolver.AppNameResolver;
import com.heroku.sdk.deploy.lib.sourceblob.JvmProjectSourceBlobCreator;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobDescriptor;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobPackager;
import com.heroku.sdk.deploy.util.Procfile;
import com.heroku.sdk.maven.MavenLogOutputAdapter;
import com.heroku.sdk.maven.MojoExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Deploys an application to Heroku without resolving dependencies, packaging, or vendoring dependencies. This
 * goal can be used to execute standalone, or be bound to a different phase in the lifecycle.
 */
@Mojo(name="deploy-only")
public class DeployOnlyMojo extends AbstractHerokuMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    OutputAdapter outputAdapter = new MavenLogOutputAdapter(getLog(), logProgress);

    Path projectDirectory = super.mavenProject.getBasedir().toPath();

    Path dependencyList;
    try {
      dependencyList = MojoExecutor.createDependencyListFile(super.mavenProject, super.mavenSession, super.pluginManager);
    } catch (IOException e) {
      throw new MojoExecutionException("Could not create dependency list file!", e);
    }

    ArrayList<Path> includedPaths = new ArrayList<>();
    Procfile defaultProcfile = Procfile.empty();

    for (String includePattern : super.includes) {
      includedPaths.addAll(resolveIncludePattern(includePattern));
    }

    includedPaths.add(Paths.get("pom.xml"));

    if (super.includeTarget) {
      includedPaths.add(Paths.get("target"));
    }

    SourceBlobDescriptor sourceBlobDescriptor;
    try {
      sourceBlobDescriptor = JvmProjectSourceBlobCreator.create(
              projectDirectory,
              "heroku-maven-plugin",
              includedPaths,
              () -> new Procfile(super.processTypes),
              defaultProcfile,
              () -> Optional.ofNullable(super.jdkVersion),
              outputAdapter
      );
    } catch (IOException e) {
      throw new MojoExecutionException("Could not create source blob descriptor!", e);
    }

    sourceBlobDescriptor.addLocalPath("mvn-dependency-list.log", dependencyList, true);

    Path sourceBlob;
    try {
      sourceBlob = SourceBlobPackager.pack(sourceBlobDescriptor, outputAdapter);
    } catch (IOException e) {
      throw new MojoExecutionException("Could not package source blob!", e);
    }

    String appName;
    try {
      appName = AppNameResolver.resolve(projectDirectory, () -> Optional.ofNullable(super.appName))
              .orElseThrow(() -> new MojoExecutionException("Could not determine app name, please configure it explicitly!"));
    } catch (IOException e) {
      throw new MojoExecutionException("Could not determine app name!", e);
    }

    DeploymentDescriptor deploymentDescriptor
            = new DeploymentDescriptor(appName, super.buildpacks, super.configVars, sourceBlob, mavenProject.getVersion());

    String apiKey;
    try {
      apiKey = ApiKeyResolver
              .resolve(projectDirectory)
              .orElseThrow(() -> new MojoExecutionException("Could not resolve API key."));
    } catch (IOException e) {
      throw new MojoExecutionException("Could not resolve API key!", e);
    }

    try {
      Deployer.deploy(apiKey, deploymentDescriptor, outputAdapter);
    } catch (IOException | InterruptedException e) {
      throw new MojoExecutionException("Could not deploy source blob!", e);
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
}
