package com.heroku.deployer.api;

public class InsufficientAppPermissionsException extends HerokuDeployApiException {
    public InsufficientAppPermissionsException(String message) {
        super(message);
    }
}
