package com.heroku.sdk.deploy.api;

public class AppNotFoundException extends HerokuDeployApiException {
    public AppNotFoundException(String message) {
        super(message);
    }
}
