package com.heroku.sdk.deploy.endpoints;

import com.heroku.sdk.deploy.utils.RestClient;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Builds extends ApiEndpoint {

  private static final String JVM_BUILDPACK_URL="https://codon-buildpacks.s3.amazonaws.com/buildpacks/heroku/jvm-common.tgz";

  private static final String METRICS_BUILDPACK_URL="https://codon-buildpacks.s3.amazonaws.com/buildpacks/heroku/metrics.tgz";

  private String blobGetUrl;

  private List<String> buildpackUrls;

  public Builds(String appName, String stackName, String commit, String encodedApiKey, List<String> buildpacks) {
    super(appName, stackName, commit, encodedApiKey);

    if (buildpacks == null || buildpacks.isEmpty()) {
      buildpackUrls = Arrays.asList(JVM_BUILDPACK_URL);
    } else {
      buildpackUrls = new ArrayList<>(buildpacks.size());
        for (String buildpack : buildpacks) {
          if (buildpack.equals("jvm-common")) {
            buildpackUrls.add(JVM_BUILDPACK_URL);
          } else if (buildpack.equals("heroku/jvm")) {
              buildpackUrls.add(JVM_BUILDPACK_URL);
          } else if (buildpack.equals("heroku/metrics")) {
            buildpackUrls.add(METRICS_BUILDPACK_URL);
          } else {
            buildpackUrls.add(buildpack);
          }
        }
    }
  }

  public Map createSource() throws IOException {
    String urlStr = BASE_URL + "/apps/" + URLEncoder.encode(appName, "UTF-8") + "/sources";
    Map sourceResponse = RestClient.post(urlStr, headers);

    Map blobJson = (Map)sourceResponse.get("source_blob");
    blobUrl = (String)blobJson.get("put_url");
    blobGetUrl = (String)blobJson.get("get_url");

    return sourceResponse;
  }

  public Map build(RestClient.OutputLogger logger) throws IOException, InterruptedException {
    if (blobGetUrl == null) {
      throw new IllegalStateException("Source must be created before releasing!");
    }

    String urlStr = BASE_URL + "/apps/" + appName + "/builds";

    String data = "{"+
        "\"buildpacks\":" + getBuildpacksJson() + ", " +
        "\"source_blob\":{\"url\":\"" + (null == blobGetUrl ? "" : StringEscapeUtils.escapeJson(blobGetUrl)) +
        "\",\"version\":\"" + (null == commit ? "" : StringEscapeUtils.escapeJson(commit)) + "\"}}";

    Map buildResponse = RestClient.post(urlStr, data, headers);

    String outputUrl = (String)buildResponse.get("output_stream_url");
    String buildId = (String)buildResponse.get("id");

    if (outputUrl != null) {
      try {
        RestClient.get(outputUrl, headers, logger);
      } catch (IOException e) {
        logger.log("Deployment output not available. Polling for status...");
      }
      return pollForBuildInfo(buildId);
    } else {
      logger.log("Deployment output not available. Polling for status...");
      return pollForBuildInfo(buildId);
    }
  }

  public Map pollForBuildInfo(String buildId) throws IOException, InterruptedException {
    for (int i = 0; i < 15; i++) {
      Thread.sleep(2000);
      Map info = getBuildInfo(buildId);
      if (!"pending".equals(info.get("status"))) {
        return info;
      }
    }
    return getBuildInfo(buildId);
  }

  public Map getBuildInfo(String buildId) throws IOException {
    String buildStatusUrlStr = BASE_URL + "/apps/" + appName + "/builds/" + buildId;
    return RestClient.get(buildStatusUrlStr, headers);
  }

  public String getBuildpacksJson() {
    String buildpacksString = "";
    for (String url : buildpackUrls) {
      buildpacksString += ",{\"url\":\"" + StringEscapeUtils.escapeJson(url) + "\"}";
    }
    return buildpacksString.replaceFirst(",", "[") + "]";
  }
}
