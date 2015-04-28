package com.heroku.sdk.deploy;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

public class Builds extends ApiEndpoint {

  private String blobGetUrl;

  public Builds(String appName, String stackName, String commit, String encodedApiKey) {
    super(appName, stackName, commit, encodedApiKey);

    headers.remove("Accept");
    headers.put("Accept", "application/vnd.heroku+json; version=3.streaming-build-output");
  }

  public void setBuildpack(String buildpackUrl) throws IOException {
    String bpUrlStr = BASE_URL + "/apps/" + URLEncoder.encode(appName, "UTF-8") + "/buildpack-installations";

    String buildpackData = "{\"updates\":[{\"buildpack\":\"" + StringEscapeUtils.escapeJson(buildpackUrl) + "\"}]}";

    RestClient.put(bpUrlStr, buildpackData, headers);
  }

  public Map createSource() throws IOException {
    String urlStr = BASE_URL + "/apps/" + URLEncoder.encode(appName, "UTF-8") + "/sources";
    Map sourceResponse = RestClient.post(urlStr, headers);

    Map blobJson = (Map)sourceResponse.get("source_blob");
    blobUrl = (String)blobJson.get("put_url");
    blobGetUrl = (String)blobJson.get("get_url");

    return sourceResponse;
  }

  public Map build(RestClient.OutputLogger logger)  throws IOException {
    if (blobGetUrl == null) {
      throw new IllegalStateException("Source must be created before releasing!");
    }

    String urlStr = BASE_URL + "/apps/" + appName + "/builds";

    String data = "{\"source_blob\":{\"url\":\"" + StringEscapeUtils.escapeJson(blobGetUrl) + "\",\"version\":\"" + StringEscapeUtils.escapeJson(commit) + "\"}}";

    Map buildResponse = RestClient.post(urlStr, data, headers);

    String outputUrl = (String)buildResponse.get("output_stream_url");

    if (outputUrl != null) {
      RestClient.get(outputUrl, headers, logger);
    }

    return buildResponse;
  }
}
