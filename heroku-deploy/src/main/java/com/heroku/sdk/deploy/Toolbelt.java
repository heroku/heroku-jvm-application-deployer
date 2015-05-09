package com.heroku.sdk.deploy;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Toolbelt {

  public static String getApiToken() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    try {
      return readNetrcFile().get("api.heroku.com").get("password");
    } catch (Throwable e) {
      return runHerokuCommand("auth:token");
    }
  }

  public static String getAppName() throws IOException, InterruptedException, ExecutionException, TimeoutException {
    String appsInfo = runHerokuCommand("apps:info -s");
    for (String line : appsInfo.split("\n")) {
      if (line.startsWith("name=")) {
        return line.replace("name=", "");
      }
    }
    return null;
  }

  private static String runHerokuCommand(final String command) throws InterruptedException, ExecutionException, TimeoutException {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    FutureTask<String> future =
        new FutureTask<String>(new Callable<String>() {
          public String call() throws IOException {
            String herokuCmd = SystemSettings.isWindows() ? "heroku.bat" : "heroku";
            ProcessBuilder pb = new ProcessBuilder().command(herokuCmd, command);
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
