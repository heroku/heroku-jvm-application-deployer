package com.heroku.sdk.deploy;

import com.heroku.sdk.deploy.standalone.StandaloneDeploy;

import java.io.IOException;

// Entry point for standalone JAR deployment. Located in this package to provide backwards comparability with 2.x.
public class DeployJar {
    public static void main(String[] args) throws IOException, InterruptedException {
        StandaloneDeploy.deploy(StandaloneDeploy.Mode.JAR);
    }
}
