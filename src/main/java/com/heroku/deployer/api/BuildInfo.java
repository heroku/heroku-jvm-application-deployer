package com.heroku.deployer.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class BuildInfo {
    @JsonProperty("id")
    public String id;

    @JsonProperty("output_stream_url")
    public String outputStreamUrl;

    @JsonProperty("status")
    public String status;

    @JsonProperty("updated_at")
    public String updatedAt;

    @Override
    public String toString() {
        return "BuildInfo{" +
                "id='" + id + '\'' +
                ", outputStreamUrl='" + outputStreamUrl + '\'' +
                ", status='" + status + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}
