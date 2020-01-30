package com.heroku.sdk.maven.mojo;

import com.heroku.sdk.deploy.lib.resolver.ApiKeyResolver;
import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.lib.deploymemt.Deployer;
import com.heroku.sdk.deploy.lib.deploymemt.DeploymentDescriptor;
import com.heroku.sdk.deploy.lib.resolver.AppNameResolver;
import com.heroku.sdk.deploy.lib.sourceblob.JvmProjectSourceBlobCreator;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobDescriptor;
import com.heroku.sdk.deploy.lib.sourceblob.SourceBlobPackager;
import com.heroku.sdk.deploy.util.Procfile;
import com.heroku.sdk.deploy.util.Util;
import com.heroku.sdk.maven.MavenLogOutputAdapter;
import com.heroku.sdk.maven.MojoExecutor;
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
@Mojo(name="deploy", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME) // TODO: Is runtime correct? Probably, because we don't need any compile dependencies.
public class DeployMojo extends AbstractHerokuMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    MojoExecutor.copyDependenciesToBuildDirectory(super.mavenProject, super.mavenSession, super.pluginManager);
    new DeployOnlyMojo().execute();
  }
}
