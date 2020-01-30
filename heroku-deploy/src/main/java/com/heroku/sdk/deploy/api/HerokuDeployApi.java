package com.heroku.sdk.deploy.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.heroku.sdk.deploy.util.Util;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jgit.util.Base64;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HerokuDeployApi {
    private Map<String, String> httpHeaders;

    public HerokuDeployApi(String client, String clientVersion, String apiKey) {
        HashMap<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put("Authorization", Base64.encodeBytes((":" + apiKey).getBytes()));
        httpHeaders.put("Content-Type", "application/json");
        httpHeaders.put("Accept", "application/vnd.heroku+json; version=3");
        httpHeaders.put("User-Agent", String.format(
                "heroku-deploy/%s (%s) Java/%s (%s)",
                clientVersion,
                client,
                System.getProperty("java.version"),
                System.getProperty("java.vendor")));

        this.httpHeaders = httpHeaders;
    }

    public BuildInfo createBuild(String appName, URI sourceBlob, String sourceBlobVersion, List<String> buildpacks) throws IOException {
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
        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost request = new HttpPost("https://api.heroku.com/apps/" + appName + "/builds");
        httpHeaders.forEach(request::setHeader);
        request.setEntity(apiPayloadEntity);

        CloseableHttpResponse response = client.execute(request);
        HttpEntity responseEntity = response.getEntity();

        if (responseEntity == null) {
            return null; // TODO: OMG!
        }

        String responseStringBody = Util.readLinesFromInputStream(responseEntity.getContent()).collect(Collectors.joining());

        return mapper.readValue(responseStringBody, BuildInfo.class);
    }

    public Stream<String> followBuildOutputStream(URI buildOutputStreamUri) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();

        HttpGet request = new HttpGet(buildOutputStreamUri);
        httpHeaders.forEach(request::setHeader);

        CloseableHttpResponse response = client.execute(request);
        HttpEntity responseEntity = response.getEntity();

        return Util.readLinesFromInputStream(responseEntity.getContent());
    }

    public BuildInfo getBuildInfo(String appName, String buildId) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        CloseableHttpClient client = HttpClients.createDefault();

        HttpUriRequest request = new HttpGet("https://api.heroku.com/apps/" + appName + "/builds/" + buildId);
        httpHeaders.forEach(request::setHeader);

        CloseableHttpResponse response = client.execute(request);
        HttpEntity responseEntity = response.getEntity();

        if (responseEntity == null) {
            return null; // TODO: OMG!
        }

        String responseStringBody = Util.readLinesFromInputStream(responseEntity.getContent()).collect(Collectors.joining());
        return mapper.readValue(responseStringBody, BuildInfo.class);
    }
}
