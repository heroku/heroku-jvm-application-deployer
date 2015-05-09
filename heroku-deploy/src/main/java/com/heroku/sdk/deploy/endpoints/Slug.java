package com.heroku.sdk.deploy.endpoints;


import com.heroku.sdk.deploy.utils.RestClient;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class Slug extends ApiEndpoint {

  private String slugId;

  private String createJson;

  public static final String BASE_URL = "https://api.heroku.com";

  public Slug(String buildPackDesc, String appName, String stack, String commit, String encodedApiKey, Map<String,String> processTypes) throws UnsupportedEncodingException {
    super(appName, stack, commit, encodedApiKey);

    if (processTypes.isEmpty()) {
      throw new MissingProcessTypesException();
    }

    createJson = "{" +
        "\"buildpack_provided_description\":\"" + StringEscapeUtils.escapeJson(buildPackDesc) +"\"," +
        "\"stack\":\"" + (stack == null ? "cedar-14" : StringEscapeUtils.escapeJson(stack)) +"\"," +
        "\"commit\":\"" + (commit == null ? "" : StringEscapeUtils.escapeJson(commit)) +"\"," +
        "\"process_types\":{";

    boolean first = true;
    for (String key : processTypes.keySet()) {
      String value = processTypes.get(key);
      if (!first) createJson += ", ";
      first = false;
      createJson += "\"" + key + "\"" + ":" + "\"" + StringEscapeUtils.escapeJson(value) + "\"";
    }
    createJson +=  "}}";
  }

  public String getSlugId() { return slugId; }
  public String getSlugRequest() { return createJson; }

  public Map create() throws IOException {
    String urlStr = BASE_URL + "/apps/" + URLEncoder.encode(appName, "UTF-8") + "/slugs";
    Map slugResponse = RestClient.post(urlStr, createJson, headers);

    Map blobJson = (Map)slugResponse.get("blob");
    blobUrl = (String)blobJson.get("url");

    slugId = (String)slugResponse.get("id");

    commit = (String)slugResponse.get("commit");

    Map stackJson = (Map)slugResponse.get("stack");
    stackName = (String)stackJson.get("name");

    return slugResponse;
  }

  public Map release() throws IOException {
    if (slugId == null) {
      throw new IllegalStateException("Slug must be created before releasing!");
    }

    String urlStr = BASE_URL + "/apps/" + appName + "/releases";

    String data = "{\"slug\":\"" + slugId + "\"}";

    return RestClient.post(urlStr, data, headers);
  }

  public static class MissingProcessTypesException extends IllegalArgumentException {}
}
