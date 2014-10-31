package com.heroku.api;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

public class Curl {
  public static String post(String urlStr, String data, Map<String,String> headers) throws IOException, CurlException {
    URL url = new URL(urlStr);
    HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
    con.setDoInput(true);
    con.setRequestMethod("POST");

    for (String key : headers.keySet()) {
      String value = headers.get(key);
      con.setRequestProperty(key, value);
    }

    return handleResponse(con);
  }

  private static String handleResponse(HttpsURLConnection con) throws IOException, CurlException {
    try {
      String output = readStream(con.getInputStream());
//      parse(output)
      return output;
    } catch (Exception e) {
      String output = readStream(con.getErrorStream());
      throw new CurlException(con.getResponseCode(), e);
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

    private int code;

    public CurlException(int code, Exception cause) {
      super("There was an exception invoking the remote service: HTTP(" + code + ")", cause);
      this.code = code;
    }

    public int getCode() {
      return code;
    }
  }
}
