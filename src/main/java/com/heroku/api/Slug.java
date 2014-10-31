package com.heroku.api;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class Slug {

  private String blobUrl;

  private String slugId;

  private String stackName;

  private String createJson;

  private String appName;

  private Map<String,String> headers;

  public Slug(String appName, String encodedApiKey, Map<String,String> processTypes) throws UnsupportedEncodingException {
    this.appName = appName;

    Map<String,String> headers = new HashMap<String,String>();
    headers.put("Authorization", encodedApiKey);
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "application/vnd.heroku+json; version=3");

    String json = "{" +
        "\"buildpack_provided_description\":\"maven-heroku-plugin\"," +
        "\"process_types\":{";

    boolean first = true;
    for (String key : processTypes.keySet()) {
      String value = processTypes.get(key);
      if (!first) json += ", ";
      first = false;
      json += "\"" + key + "\"" + ":" + "\"" + sanitizeJson(value) + "\"";
    }
    json +=  "}}";

    this.createJson = json;
  }

  public void create(String data) throws IOException, Curl.CurlException {
    // invoke api with createJson
    // store blobUrl

    String urlStr = "https://api.heroku.com/apps/" + URLEncoder.encode(appName, "UTF-8") + "/slugs";
    String slugResponse = Curl.post(urlStr, data, headers);

    // parse for blobUrl and slugId


  }

  public void upload(File slugFile) throws IOException {
    if (blobUrl == null) {
      throw new IllegalStateException("Slug must be created before uploading!");
    }

    URL url = new URL(blobUrl);
    HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();

    connection.setDoOutput(true);
    connection.setRequestMethod("PUT");
    connection.setConnectTimeout(0);
    connection.setRequestProperty("Content-Type", "");

    connection.connect();
    OutputStream out = connection.getOutputStream();
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(slugFile));

    byte[] buffer = new byte[1024];
    int length = in.read(buffer);
    while (length != -1) {
      out.write(buffer, 0, length);
      out.flush();
      length = in.read(buffer);
    }
    out.close();
    in.close();
    int responseCode = connection.getResponseCode();

    if (responseCode != 200) {
      throw new RuntimeException("Failed to upload slug (HTTP/1.1 " + responseCode + ")");
    }
  }

  public void release() throws IOException, Curl.CurlException {
    if (slugId == null) {
      throw new IllegalStateException("Slug must be created before uploading!");
    }

    String urlStr = "https://api.heroku.com/apps/" + appName + "/releases";

    String data = "{\"slug\":\"" + slugId + "\"}";

    Curl.post(urlStr, data, headers);
  }

  private String sanitizeJson(String json) {
    return json.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
