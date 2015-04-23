package com.heroku.sdk.deploy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heroku.sdk.deploy.utils.UploadListener;
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RestClient {
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

  public static void put(String urlStr, File file, UploadListener uploadListener) throws IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
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

  private static class FileEntityWithProgress extends FileEntity {

    private OutputStreamProgress outStream;

    private final UploadListener uploadListener;

    private Executor executor = Executors.newSingleThreadExecutor();

    public FileEntityWithProgress(File file, UploadListener uploadListener) {
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
      if (uploadListener.isEnabled() && contentLength > 0) {
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
