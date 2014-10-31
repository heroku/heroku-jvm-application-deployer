package com.heroku.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

public class Curl {
  public static Map post(String urlStr, String data, Map<String,String> headers) throws IOException, CurlException {
    URL url = new URL(urlStr);
    HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
    con.setDoInput(true);
    con.setDoOutput(true);
    con.setRequestMethod("POST");

    for (String key : headers.keySet()) {
      String value = headers.get(key);
      con.setRequestProperty(key, value);
    }

    con.getOutputStream().write(data.getBytes("UTF-8"));

    return handleResponse(con);
  }

  private static Map handleResponse(HttpsURLConnection con) throws IOException, CurlException {
    try {
      String output = readStream(con.getInputStream());
      return (new ObjectMapper()).readValue(output, Map.class);
    } catch (Exception e) {
      String output = readStream(con.getErrorStream());
      throw new CurlException(con.getResponseCode(), output, e);
    }
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

  public static class CurlException extends Exception {

    private Integer code;

    private String response;

    public CurlException(Integer code, String response, Exception cause) {
      super("There was an exception invoking the remote service: HTTP(" + code + ")", cause);
      this.code = code;
      this.response = response;
    }

    public Integer getCode() {
      return code;
    }

    public String getReponse() {
      return response;
    }
  }
}
