package com.heroku.sdk.deploy.api;

public class SourceBlob {
    private final String putUrl;
    private final String getUrl;

    public SourceBlob(String putUrl, String getUrl) {
        this.putUrl = putUrl;
        this.getUrl = getUrl;
    }

    public String getPutUrl() {
        return putUrl;
    }

    public String getGetUrl() {
        return getUrl;
    }
}
