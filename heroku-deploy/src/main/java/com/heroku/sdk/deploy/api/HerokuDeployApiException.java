package com.heroku.sdk.deploy.api;

public class HerokuDeployApiException extends Exception {
    public HerokuDeployApiException(String message) {
        super(message);
    }

    public HerokuDeployApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
