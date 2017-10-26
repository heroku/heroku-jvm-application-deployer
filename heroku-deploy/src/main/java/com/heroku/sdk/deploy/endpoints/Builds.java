package com.heroku.sdk.deploy.endpoints;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.heroku.api.BuildpackInstallation;
import com.heroku.api.HerokuAPI;
import com.heroku.sdk.deploy.utils.Curl;
import com.heroku.sdk.deploy.utils.Logger;
import com.heroku.sdk.deploy.utils.RestClient;
import org.apache.commons.lang3.StringEscapeUtils;

public class Builds extends ApiEndpoint {

  public static final String JVM_BUILDPACK_URL="https://codon-buildpacks.s3.amazonaws.com/buildpacks/heroku/jvm-common.tgz";

  public static final String METRICS_BUILDPACK_URL="https://codon-buildpacks.s3.amazonaws.com/buildpacks/heroku/metrics.tgz";

  public static final String EXEC_BUILDPACK_URL="https://codon-buildpacks.s3.amazonaws.com/buildpacks/heroku/exec.tgz";

  private String blobGetUrl;

  private List<String> buildpackUrls;

  private String commit;

  public Builds(String appName, String client, String commit, String apiKey, List<String> buildpacks) throws IOException {
    super(appName, client, apiKey);
    this.commit = commit;

    if (buildpacks == null || buildpacks.isEmpty()) {
      HerokuAPI api = new HerokuAPI(apiKey);
      List<BuildpackInstallation> buildpackInstalls = api.listBuildpackInstallations(appName);

      if (buildpackInstalls.isEmpty()) {
        buildpackUrls = Collections.singletonList(JVM_BUILDPACK_URL);
      } else if (containsJvmBuildpack(buildpackInstalls)) {
        buildpackUrls = new ArrayList<>();
        for (BuildpackInstallation buildpack : buildpackInstalls) {
          buildpackUrls.add(resolveBuildpack(buildpack.getBuildpack().getName()));
        }
      } else {
        throw new IllegalArgumentException("Your buildpacks do not contain the heroku/jvm buildpack!" +
            "Add heroku/jvm to your buildpack configuration or run `heroku buildpacks:clear`.");
      }
    } else {
      buildpackUrls = new ArrayList<>(buildpacks.size());
        for (String buildpack : buildpacks) {
          buildpackUrls.add(resolveBuildpack(buildpack));
        }
    }
  }

  public void upload(File slugFile, Logger listener) throws IOException, InterruptedException {
    if (blobUrl == null) {
      throw new IllegalStateException("Source must be created before uploading!");
    }

    if (useCurl) {
      listener.logDebug("Uploading with curl");
      Curl.put(blobUrl, slugFile);
    } else {
      RestClient.put(blobUrl, slugFile, listener);
    }
  }

  public String getBlobUrl() {
    return blobUrl;
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

  private Boolean containsJvmBuildpack(List<BuildpackInstallation> buildpackInstalls) {
    for (BuildpackInstallation buildpack : buildpackInstalls) {
      if (buildpack.getBuildpack().getName().startsWith("heroku/jvm")) {
        return true;
      } else if (buildpack.getBuildpack().getName().startsWith("https://github.com/heroku/heroku-buildpack-jvm-common")) {
        return true;
      } else if (buildpack.getBuildpack().getName().startsWith("https://codon-buildpacks.s3.amazonaws.com/buildpacks/heroku/jvm-common.tgz")) {
        return true;
      }
    }
    return false;
  }

  private String resolveBuildpack(String buildpack) {
    if (buildpack.equals("jvm-common")) {
      return JVM_BUILDPACK_URL;
    } else if (buildpack.equals("heroku/jvm")) {
      return JVM_BUILDPACK_URL;
    } else if (buildpack.equals("heroku/metrics")) {
      return METRICS_BUILDPACK_URL;
    } else if (buildpack.equals("heroku/exec")) {
      return EXEC_BUILDPACK_URL;
    } else {
      return buildpack;
    }
  }
}
