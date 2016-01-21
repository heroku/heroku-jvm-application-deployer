package com.heroku.sdk.deploy.utils;

import java.io.File;
import java.io.IOException;

public class Curl {

  public static void put(String urlStr, File file) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder().command("curl", "-T", file.getAbsolutePath(), "-L", urlStr);
    pb.start().waitFor();
  }
}

