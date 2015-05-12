package com.heroku.sdk.deploy;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Toolbelt {

  public static String getApiToken() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    try {
      return readNetrcFile().get("api.heroku.com").get("password");
    } catch (Throwable e) {
      return runHerokuCommand(new File(System.getProperty("user.home"), "auth:token"));
    }
  }

  public static String getAppName(File projectDir) throws IOException, InterruptedException, ExecutionException, TimeoutException {
    Map<String,String> remotes = getGitRemotes(projectDir);
    if (remotes.containsKey("heroku")) {
      return parseAppFromRemote(remotes.get("heroku"));
    } else {
      throw new RuntimeException("No 'heroku' remote found.");
    }
  }

  private static String runHerokuCommand(final File projectDir, final String... command) throws InterruptedException, ExecutionException, TimeoutException {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    FutureTask<String> future =
        new FutureTask<String>(new Callable<String>() {
          public String call() throws IOException {
            String herokuCmd = SystemSettings.isWindows() ? "heroku.bat" : "heroku";

            // crazy Java
            String[] fullCommand = new String[command.length + 1];
            fullCommand[0] = herokuCmd;
            System.arraycopy(command, 0, fullCommand, 1, command.length);

            ProcessBuilder pb = new ProcessBuilder().command(fullCommand);
            pb.directory(projectDir);
            Process p = pb.start();

            BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            String output = "";
            while ((line = bri.readLine()) != null) {
              output += line;
            }
            return output;
          }});

    executor.execute(future);

    return future.get(10, TimeUnit.SECONDS);
  }

  private static Map<String,String> getGitRemotes(File projectDir) throws IOException {
    File gitConfigFile = new File(new File(projectDir, ".git"), "config");

    if (!gitConfigFile.exists()) {
      throw new FileNotFoundException(gitConfigFile.toString());
    }

    Map<String,String> remotes = new HashMap<String, String>();

    String remote = null;
    for (String line : FileUtils.readLines(gitConfigFile)) {
      if (line != null && !line.trim().isEmpty()) {
        if (line.startsWith("[remote")) {
          remote = line.replace("[remote \"", "").replace("\"]", "");
        } else if (remote != null && line.contains("url =")) {
          String[] keyValue = line.trim().split("=");
          remotes.put(remote, keyValue[1].trim());
        }
      }
    }

    return remotes;
  }

  private static String parseAppFromRemote(String remote) {
    if (remote.startsWith("https")) {
      return remote.replace("https://git.heroku.com/", "").replace(".git", "");
    } else if (remote.startsWith("git")) {
      return remote.replace("git@heroku.com:", "").replace(".git", "");
    }
    return null;
  }

  private static Map<String,Map<String,String>> readNetrcFile() throws IOException {
    String homeDir = System.getProperty("user.home");
    String netrcFilename = SystemSettings.isWindows() ? "_netrc" : ".netrc";
    File netrcFile = new File(new File(homeDir), netrcFilename);

    if (!netrcFile.exists()) {
      throw new FileNotFoundException(netrcFile.toString());
    }

    Map<String,Map<String,String>> netrcMap = new HashMap<String, Map<String, String>>();

    String machine = null;
    Map<String,String> entry = new HashMap<String, String>();
    for (String line : FileUtils.readLines(netrcFile)) {
      if (line != null && !line.trim().isEmpty()) {
        if (line.startsWith("machine")) {
          if (null != machine) {
            netrcMap.put(machine, entry);
            entry = new HashMap<String, String>();
          }
          machine = line.trim().split(" ")[1];
        } else {
          String[] keyValue = line.trim().split(" ");
          entry.put(keyValue[0], keyValue[1]);
        }
      }
    }

    if (null != machine) {
      netrcMap.put(machine, entry);
    }

    return netrcMap;
  }
}
