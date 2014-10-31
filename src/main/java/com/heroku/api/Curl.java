package com.heroku.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
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

  public static void put(String urlStr, File file) throws IOException, CurlException {
    URL url = new URL(urlStr);
    HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();

    connection.setDoOutput(true);
    connection.setRequestMethod("PUT");
    connection.setConnectTimeout(0);
    connection.setRequestProperty("Content-Type", "");

    connection.connect();
    OutputStream out = connection.getOutputStream();
    BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));

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
      throw new CurlException(responseCode, "Failed to upload slug!");
    }
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

    public CurlException(Integer code, String response) {
      super("There was an exception invoking the remote service: HTTP(" + code + ")");
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
