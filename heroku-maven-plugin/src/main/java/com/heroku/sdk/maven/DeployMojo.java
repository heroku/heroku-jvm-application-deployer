package com.heroku.sdk.maven;

import com.heroku.sdk.deploy.api.ApiKeyResolver;
import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.lib.deploy.Deployer;
import com.heroku.sdk.deploy.lib.deploy.DeploymentDescriptor;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobContent;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobPackager;
import com.heroku.sdk.deploy.util.Procfile;
import com.heroku.sdk.deploy.util.Util;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deploys an application to Heroku.
 */
@Mojo(name="deploy", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE) // TODO: Compile correct?
public class DeployMojo extends AbstractHerokuMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    OutputAdapter outputAdapter = new MavenLogOutputAdapter(getLog(), logProgress);

    try {
      Path dependencyList = MojoExecutor.createDependencyListFile(super.mavenProject, super.mavenSession, super.pluginManager);
      MojoExecutor.copyDependenciesToBuildDirectory(super.mavenProject, super.mavenSession, super.pluginManager);

      ArrayList<SourceBlobContent> contents = new ArrayList<>();
      contents.add(new SourceBlobContent(dependencyList, Paths.get("foo.log")));

      contents.add(new SourceBlobContent(Util.createTemporaryFileWithStringContents(buildProcfile().asString()), "Procfile"));
      contents.add(new SourceBlobContent(Util.createTemporaryFileWithStringContents("client=heroku-maven-plugin"), ".heroku-deploy"));
      contents.add(new SourceBlobContent(mavenProject.getBasedir().toPath().resolve("pom.xml"), "pom.xml"));

      for (String includePattern : super.includes) {
        for (Path localPath : resolveIncludePattern(includePattern)) {
          Path sourceBlobPath = mavenProject.getBasedir().toPath().relativize(localPath);
          contents.add(new SourceBlobContent(localPath, sourceBlobPath));
        }
      }

      if (super.includeTarget) {
        contents.add(new SourceBlobContent(mavenProject.getBasedir().toPath().resolve("target"), Paths.get("target")));
      }

      Path sourceBlob = SourceBlobPackager.pack(contents, outputAdapter);

      DeploymentDescriptor deploymentDescriptor
              = new DeploymentDescriptor(super.appName, super.buildpacks, sourceBlob, Optional.of(mavenProject.getVersion()));

      String apiKey = ApiKeyResolver
              .resolve(Paths.get(mavenSession.getExecutionRootDirectory()))
              .orElseThrow(() -> new MojoExecutionException("Could not resolve API key."));

      Deployer.deploy(apiKey, deploymentDescriptor, outputAdapter);

    } catch (IOException e) {
      throw new MojoExecutionException("Could not create dependency list file!", e);
    } catch (InterruptedException e) {
      throw new MojoExecutionException("Could not deploy source blob!", e);
    }
  }

  private Procfile buildProcfile() throws IOException {
    Procfile procfileFromFile = Procfile.fromFile(Paths.get(mavenSession.getExecutionRootDirectory(), "Procfile"));
    Procfile procfileFromProcessTypes = new Procfile(super.processTypes);
    return procfileFromFile.merge(procfileFromProcessTypes);
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
