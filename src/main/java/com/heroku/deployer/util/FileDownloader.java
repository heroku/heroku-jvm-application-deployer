package com.heroku.deployer.util;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileDownloader {

    public static Path download(URI uri) throws IOException {
        Path temporaryFilePath = Files.createTempFile("heroku-deploy-file-downloader", ".tmp");

        HttpGet request = new HttpGet(uri);

        try (CloseableHttpClient client = HttpClients.createSystem();
             CloseableHttpResponse response = client.execute(request)) {

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                Files.deleteIfExists(temporaryFilePath);
                throw new IOException(String.format("Unexpected status code downloading %s: %d", uri, statusCode));
            }

            HttpEntity entity = response.getEntity();
            try (OutputStream out = Files.newOutputStream(temporaryFilePath)) {
                entity.writeTo(out);
            }
        } catch (IOException e) {
            Files.deleteIfExists(temporaryFilePath);
            throw e;
        }

        return temporaryFilePath;
    }
}
