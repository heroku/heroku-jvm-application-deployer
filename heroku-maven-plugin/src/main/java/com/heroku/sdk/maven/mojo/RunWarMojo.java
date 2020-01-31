package com.heroku.sdk.maven.mojo;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts the web application in a way that is very similar to how it is run on Heroku.
 */
@Mojo(name = "run-war", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class RunWarMojo extends AbstractHerokuMojo {

  @Override
  public void execute() throws MojoFailureException {
    /*try {
      prepareWarFile();

      String javaCommand = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

      String javaOpts = configVars.get("JAVA_OPTS");
      String webappRunnerOpts = configVars.get("WEBAPP_RUNNER_OPTS");

      List<String> fullCommand = new ArrayList<String>();
      fullCommand.add(javaCommand);
      if (null != javaOpts) {
        for (String javaOpt : javaOpts.split(" ")) {
          if (!javaOpt.isEmpty()) fullCommand.add(javaOpt);
        }
      }

      fullCommand.add("-jar");
      fullCommand.add("target" + File.separator + "dependency" + File.separator + "webapp-runner.jar");
      if (null != webappRunnerOpts) {
        for (String webappRunnerOpt : webappRunnerOpts.split(" ")) {
          if (!webappRunnerOpt.isEmpty()) fullCommand.add(webappRunnerOpt);
        }
      }

      fullCommand.add(warFile.getAbsolutePath());

      ProcessBuilder pb = new ProcessBuilder().command(fullCommand.toArray(new String[fullCommand.size()]));

      String fullCommandLine = javaCommand;
      for (String s : fullCommand) {
        fullCommandLine += " " + s;
      }
      getLog().debug("Executing: " + fullCommandLine);

      Process p = pb.start();

      StreamGobbler inputGobbler = new StreamGobbler(p.getInputStream());
      inputGobbler.start();

      StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream());
      errorGobbler.start();

      p.waitFor();
    } catch (Exception e) {
      throw new MojoFailureException("Failed to deploy application", e);
    }
  }

  private class StreamGobbler extends Thread {
    InputStream is;

    StreamGobbler(InputStream is) {
      this.is = is;
    }

    public void run() {
      try {
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null)
          getLog().info(line);
      } catch (IOException e) {
        getLog().error(e.getMessage());
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }*/
  }
}