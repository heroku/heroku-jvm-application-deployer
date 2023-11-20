package com.heroku.sdk.deploy.api;

public class InsufficientAppPermissionsException extends HerokuDeployApiException {
    public InsufficientAppPermissionsException(String message) {
        super(message);
    }
}
