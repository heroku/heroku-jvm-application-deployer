package com.heroku.sdk.deploy.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RestClient {

  private static CloseableHttpClient createClientWithProxy(String proxyStr) throws URISyntaxException {
    URI proxyUri = new URI(proxyStr);
    HttpHost proxy = new HttpHost(proxyUri.getHost(), proxyUri.getPort(), proxyUri.getScheme());
    DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
    return HttpClients.custom()
        .setRoutePlanner(routePlanner)
        .build();
  }

  private static CloseableHttpClient createClient() {
    String httpProxy = System.getenv("HTTP_PROXY");
    String httpsProxy = System.getenv("HTTPS_PROXY");

    if (httpsProxy != null) {
      try {
        return createClientWithProxy(httpsProxy);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException("HTTPS_PROXY is not valid!" , e);
      }
    } else if (httpProxy != null) {
      try {
        return createClientWithProxy(httpProxy);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException("HTTP_PROXY is not valid!" , e);
      }
    } else {
      return HttpClients.createDefault();
    }
  }

  public static Map get(String urlStr, Map<String,String> headers) throws IOException {
    CloseableHttpClient httpClient = createClient();
    HttpGet request = new HttpGet(urlStr);
    for (String key : headers.keySet()) {
      String value = headers.get(key);
      request.setHeader(key, value);
    }
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return (Map)handleResponse(response, Map.class);
    }
  }

  public static void get(String urlStr, Map<String,String> headers, OutputLogger logger) throws IOException {
    CloseableHttpClient httpClient = createClient();
    HttpGet request = new HttpGet(urlStr);
    for (String key : headers.keySet()) {
      String value = headers.get(key);
      request.setHeader(key, value);
    }
    try (CloseableHttpResponse response = httpClient.execute(request)) {
      handleResponse(response, logger);
    }
  }

  public static List put(String urlStr, String data, Map<String,String> headers) throws IOException {
    CloseableHttpClient httpClient = createClient();
    HttpPut request = new HttpPut(urlStr);
    for (String key : headers.keySet()) {
      String value = headers.get(key);
      request.setHeader(key, value);
    }

    StringEntity body = new StringEntity(data);
    body.setContentType("application/json");
    body.setContentEncoding("UTF-8");
    request.setEntity(body);

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return (List)handleResponse(response, List.class);
    }
  }

  public static Map post(String urlStr, Map<String,String> headers) throws IOException {
    CloseableHttpClient httpClient = createClient();
    HttpPost request = new HttpPost(urlStr);
    for (String key : headers.keySet()) {
      String value = headers.get(key);
      request.setHeader(key, value);
    }

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      return (Map)handleResponse(response, Map.class);
    }
  }

  public static Map post(String urlStr, String data, Map<String,String> headers) throws IOException {
    CloseableHttpClient httpClient = createClient();
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
      return (Map)handleResponse(response, Map.class);
    }
  }

  public static Map patch(String urlStr, String data, Map<String,String> headers) throws IOException {
    CloseableHttpClient httpClient = createClient();
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
      return (Map)handleResponse(response, Map.class);
    }
  }

  public static void put(String urlStr, File file, Logger uploadListener) throws IOException {
    CloseableHttpClient httpClient = createClient();
    HttpPut request = new HttpPut(urlStr);

    FileEntityWithProgress body = new FileEntityWithProgress(file, uploadListener);
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

  private static Object handleResponse(CloseableHttpResponse response, Class returnType) throws IOException {
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
    return (new ObjectMapper()).readValue(output, returnType);
  }

  private static void handleResponse(CloseableHttpResponse response, OutputLogger logger) throws IOException {
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

    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
    String tmp = reader.readLine();
    while (tmp != null) {
      logger.log(tmp);
      tmp = reader.readLine();
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

  public static abstract class OutputLogger {
    public abstract void log(String line);
  }

  private static class FileEntityWithProgress extends FileEntity {

    private OutputStreamProgress outStream;

    private final Logger uploadListener;

    private Executor executor = Executors.newSingleThreadExecutor();

    public FileEntityWithProgress(File file, Logger uploadListener) {
      super(file);
      this.uploadListener = uploadListener;
    }

    @Override
    public void writeTo(OutputStream outStream) throws IOException {
      this.outStream = new OutputStreamProgress(outStream, this);
      super.writeTo(this.outStream);
    }

    public void updateProgress(final long writtenLength) {
      final long contentLength = getContentLength();
      if (uploadListener.isUploadProgressEnabled() && contentLength > 0) {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            uploadListener.logUploadProgress(writtenLength, contentLength);
          }
        });
      }
    }
  }

  private static class OutputStreamProgress extends OutputStream {

    private OutputStream outStream;

    private FileEntityWithProgress fileEntity;

    private volatile long bytesWritten = 0;

    public OutputStreamProgress(OutputStream outStream, FileEntityWithProgress fileEntity) {
      this.outStream = outStream;
      this.fileEntity = fileEntity;
    }

    @Override
    public void write(int b) throws IOException {
      outStream.write(b);
      bytesWritten++;
      fileEntity.updateProgress(getWrittenLength());
    }

    @Override
    public void write(byte[] b) throws IOException {
      outStream.write(b);
      bytesWritten += b.length;
      fileEntity.updateProgress(getWrittenLength());
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      outStream.write(b, off, len);
      bytesWritten += len;
      fileEntity.updateProgress(getWrittenLength());
    }

    @Override
    public void flush() throws IOException {
      outStream.flush();
    }

    @Override
    public void close() throws IOException {
      outStream.close();
    }

    public long getWrittenLength() {
      return bytesWritten;
    }
  }
}
