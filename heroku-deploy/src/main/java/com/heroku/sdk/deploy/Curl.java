package com.heroku.sdk.deploy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.util.Map;

public class Curl {
  public static Map get(String urlStr, Map<String,String> headers) throws IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpGet request = new HttpGet(urlStr);
    for (String key : headers.keySet()) {
      String value = headers.get(key);
      request.setHeader(key, value);
    }
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return handleResponse(response);
    }
  }

  public static Map post(String urlStr, String data, Map<String,String> headers) throws IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost request = new HttpPost(urlStr);
    for (String key : headers.keySet()) {
      String value = headers.get(key);
      request.setHeader(key, value);
    }

    StringEntity body = new StringEntity(data);
    body.setContentType("application/json");
    body.setContentEncoding("UTF-8");
    request.setEntity(body);

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return handleResponse(response);
    }
  }

  public static Map patch(String urlStr, String data, Map<String,String> headers) throws IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPatch request = new HttpPatch(urlStr);
    for (String key : headers.keySet()) {
      String value = headers.get(key);
      request.setHeader(key, value);
    }

    StringEntity body = new StringEntity(data);
    body.setContentType("application/json");
    body.setContentEncoding("UTF-8");
    request.setEntity(body);

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return handleResponse(response);
    }
  }

  public static void put(String urlStr, File file) throws IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPut request = new HttpPut(urlStr);

    FileEntity body = new FileEntity(file);
    request.setEntity(body);

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      StatusLine statusLine = response.getStatusLine();
      if (statusLine.getStatusCode() >= 300) {
        throw new HttpResponseException(
            statusLine.getStatusCode(),
            statusLine.getReasonPhrase());
      }
    }
  }

  private static Map handleResponse(CloseableHttpResponse response) throws IOException {
    StatusLine statusLine = response.getStatusLine();
    HttpEntity entity = response.getEntity();
    if (statusLine.getStatusCode() >= 300) {
      throw new HttpResponseException(
          statusLine.getStatusCode(),
          statusLine.getReasonPhrase());
    }
    if (entity == null) {
      throw new ClientProtocolException("Response contains no content");
    }
    String output = readStream(entity.getContent());
    return (new ObjectMapper()).readValue(output, Map.class);
  }

  private static String readStream(InputStream is) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String output = "";
    String tmp = reader.readLine();
    while (tmp != null) {
      output += tmp;
      tmp = reader.readLine();
    }
    return output;
  }
}
