package com.heroku.sdk.deploy;

import com.heroku.sdk.deploy.utils.Logger;

import java.io.File;

public class SlugDeployer extends Deployer {
  public SlugDeployer(String buildPackDesc,String name, File rootDir, File targetDir, Logger logger) {
    super(buildPackDesc, name, rootDir, targetDir, logger);
  }
}
