package com.heroku.maven;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class Deploy {

  private File baseDirectory;
  private File targetDir;
  private URL jdkUrl;
  private String appName;
  private Map<String,String> configVars;
  private Map<String,String> procTypes;
  private List<String> includePaths;

  public Deploy() {

  }

  public void execute() {

  }
}
