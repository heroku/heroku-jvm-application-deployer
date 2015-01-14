package com.heroku.sdk.maven;

import com.heroku.sdk.maven.executor.CopyDependencies;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Creates a Slug to upload to Heroku
 *
 * @goal create-slug
 * @execute phase="package"
 * @requiresDependencyResolution
 */
public class CreateSlugMojo extends HerokuMojo {

    /**
     * The process types used to run on Heroku (similar to Procfile).
     *
     * @required
     * @parameter property="heroku.processTypes"
     */
    protected Map<String,String> processTypes = null;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        CopyDependencies.execute(this.mavenProject, this.mavenSession, this.pluginManager);

        List<File> includedDirs = getIncludes();
        includedDirs.add(getTargetDir());

        try {
            (new MavenApp(appName, getTargetDir().getParentFile(), getTargetDir(), getLog()))
                    .createSlugFile(slugFileName, includedDirs, getConfigVars(), jdkUrl == null ? jdkVersion : jdkUrl, stack);
        } catch (Exception e) {
            throw new MojoFailureException("Failed to create Slug", e);
        }
    }
}
