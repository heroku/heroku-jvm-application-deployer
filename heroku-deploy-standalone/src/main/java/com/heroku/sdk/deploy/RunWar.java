package com.heroku.sdk.deploy;

import com.heroku.sdk.deploy.lib.OutputAdapter;
import com.heroku.sdk.deploy.lib.running.RunWebApp;
import com.heroku.sdk.deploy.standalone.StdOutOutputAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

// Entry point for running a WAR locally. Located in this package to be consistent with DeployJar and DeployWar.
public class RunWar {
    public static void main(String[] args) throws IOException, InterruptedException {
        final OutputAdapter outputAdapter = new StdOutOutputAdapter(false);

        final String herokuWarFileSystemProperty = System.getProperty("heroku.warFile");
        if (herokuWarFileSystemProperty == null ) {
            outputAdapter.logError("Path to WAR file must be provided with heroku.warFile system property!");
            System.exit(-1);
        }

        final Path warFilePath = Paths.get(herokuWarFileSystemProperty);
        if (!Files.exists(warFilePath)) {
            outputAdapter.logError(String.format("Could not find WAR file: %s.", warFilePath));
            System.exit(-1);
        }

        final String webAppRunnerVersion
                = System.getProperty("heroku.webappRunnerVersion", Constants.DEFAULT_WEBAPP_RUNNER_VERSION);

        // Currently there is no support for adding java or webapp runner options. If you need more fine-tuned
        // parameters, use webapp-runner directly.
        final List<String> javaOptions = Collections.emptyList();
        final List<String> webappRunnerOptions = Collections.emptyList();

        RunWebApp.run(warFilePath, javaOptions, webappRunnerOptions, webAppRunnerVersion, outputAdapter);
    }
}
