package com.heroku.deployer.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.heroku.deployer.util.CustomHttpClientBuilder;
import com.heroku.deployer.util.PropertiesUtils;
import com.heroku.deployer.util.Util;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.jgit.util.Base64;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HerokuDeployApi {
    private Map<String, String> httpHeaders;

    public HerokuDeployApi(String client, String clientVersion, String apiKey) {
        Properties pomProperties
                = PropertiesUtils.loadPomPropertiesOrEmptyFromClasspath(this.getClass(), "com.heroku.sdk", "heroku-deploy");

        HashMap<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("Authorization", Base64.encodeBytes((":" + apiKey).getBytes()));
        httpHeaders.put("Content-Type", "application/json");
        httpHeaders.put("Accept", "application/vnd.heroku+json; version=3");
        httpHeaders.put("User-Agent", String.format(
                "heroku-deploy/%s (%s/%s) Java/%s (%s)",
                pomProperties.getProperty("version", "unknown"),
                client,
                clientVersion,
                System.getProperty("java.version"),
                System.getProperty("java.vendor")));

        this.httpHeaders = httpHeaders;
    }

    public SourceBlob createSourceBlob() throws IOException, HerokuDeployApiException {
        CloseableHttpClient client = CustomHttpClientBuilder.build();

        HttpPost request = new HttpPost("https://api.heroku.com/sources");
        httpHeaders.forEach(request::setHeader);

        CloseableHttpResponse response = client.execute(request);

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_ACCEPTED:
            case HttpStatus.SC_CREATED:
                HttpEntity responseEntity = response.getEntity();
                String responseStringBody = Util.readLinesFromInputStream(responseEntity.getContent()).collect(Collectors.joining());

                JsonNode node = new ObjectMapper().readTree(responseStringBody);
                String putUrl = node.get("source_blob").get("put_url").asText();
                String getUrl = node.get("source_blob").get("get_url").asText();

                return new SourceBlob(putUrl, getUrl);

            default:
                throw new HerokuDeployApiException(String.format("Unexpected status code: %d!", response.getStatusLine().getStatusCode()));
        }
    }

    public void updateAppConfig(String appName, Map<String, String> config) throws IOException, HerokuDeployApiException {
        // Create API payload
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        for (Map.Entry<String, String> entry : config.entrySet()) {
            root.put(entry.getKey(), entry.getValue());
        }

        StringEntity apiPayloadEntity = new StringEntity(root.toString());
        apiPayloadEntity.setContentType("application/json");
        apiPayloadEntity.setContentEncoding("UTF-8");

        // Send request
        CloseableHttpClient client = CustomHttpClientBuilder.build();

        HttpPatch request = new HttpPatch("https://api.heroku.com/apps/" + appName + "/config-vars");
        httpHeaders.forEach(request::setHeader);
        request.setEntity(apiPayloadEntity);

        CloseableHttpResponse response = client.execute(request);

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                return;

            default:
                throw new HerokuDeployApiException(String.format("Unexpected status code: %d!", response.getStatusLine().getStatusCode()));
        }
    }

    public BuildInfo createBuild(String appName, URI sourceBlob, String sourceBlobVersion, List<String> buildpacks) throws IOException, HerokuDeployApiException {
        // Create API payload
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        ObjectNode sourceBlobObject = root.putObject("source_blob");
        sourceBlobObject.put("url", sourceBlob.toString());
        sourceBlobObject.put("version", sourceBlobVersion);

        ArrayNode buildpacksArray = root.putArray("buildpacks");
        buildpacks.forEach(buildpackString -> {
            ObjectNode buildpackObjectNode = buildpacksArray.addObject();

            if (buildpackString.startsWith("http")) {
                buildpackObjectNode.put("url", buildpackString);
            } else {
                buildpackObjectNode.put("name", buildpackString);
            }
        });

        StringEntity apiPayloadEntity = new StringEntity(root.toString());
        apiPayloadEntity.setContentType("application/json");
        apiPayloadEntity.setContentEncoding("UTF-8");

        // Send request
        CloseableHttpClient client = CustomHttpClientBuilder.build();

        HttpPost request = new HttpPost("https://api.heroku.com/apps/" + appName + "/builds");
        httpHeaders.forEach(request::setHeader);
        request.setEntity(apiPayloadEntity);

        CloseableHttpResponse response = client.execute(request);

        return handleBuildInfoResponse(appName, mapper, response);
    }

    public BuildInfo getBuildInfo(String appName, String buildId) throws IOException, HerokuDeployApiException {
        ObjectMapper mapper = new ObjectMapper();
        CloseableHttpClient client = CustomHttpClientBuilder.build();

        HttpUriRequest request = new HttpGet("https://api.heroku.com/apps/" + appName + "/builds/" + buildId);
        httpHeaders.forEach(request::setHeader);

        CloseableHttpResponse response = client.execute(request);

        return handleBuildInfoResponse(appName, mapper, response);
    }

    public Stream<String> followBuildOutputStream(URI buildOutputStreamUri) throws IOException {
        CloseableHttpClient client = CustomHttpClientBuilder.build();

        HttpGet request = new HttpGet(buildOutputStreamUri);
        httpHeaders.forEach(request::setHeader);

        CloseableHttpResponse response = client.execute(request);
        HttpEntity responseEntity = response.getEntity();

        return Util.readLinesFromInputStream(responseEntity.getContent());
    }

    private BuildInfo handleBuildInfoResponse(String appName, ObjectMapper mapper, CloseableHttpResponse response) throws IOException, HerokuDeployApiException {
        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_NOT_FOUND:
                throw new AppNotFoundException(String.format("App %s could not be found!", appName));

            case HttpStatus.SC_FORBIDDEN:
                throw new InsufficientAppPermissionsException(String.format("Could not access app %s: insufficient permissions", appName));

            case HttpStatus.SC_OK:
            case HttpStatus.SC_CREATED:
                HttpEntity responseEntity = response.getEntity();
                String responseStringBody = Util.readLinesFromInputStream(responseEntity.getContent()).collect(Collectors.joining());

                return mapper.readValue(responseStringBody, BuildInfo.class);

            default:
                throw new HerokuDeployApiException(String.format("Unexpected status code: %d!", response.getStatusLine().getStatusCode()));
        }
    }
}
