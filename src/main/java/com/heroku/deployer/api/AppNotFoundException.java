package com.heroku.deployer.api;

public class AppNotFoundException extends HerokuDeployApiException {
    public AppNotFoundException(String message) {
        super(message);
    }
}
