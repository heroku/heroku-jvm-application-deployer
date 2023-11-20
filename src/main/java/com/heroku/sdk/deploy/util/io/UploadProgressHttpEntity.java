package com.heroku.sdk.deploy.util.io;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public class UploadProgressHttpEntity implements HttpEntity {
    private HttpEntity wrappedEntity;
    private Consumer<Long> progressConsumer;

    public UploadProgressHttpEntity(HttpEntity wrappedEntity, Consumer<Long> progressConsumer) {
        this.wrappedEntity = wrappedEntity;
        this.progressConsumer = progressConsumer;
    }

    @Override
    public boolean isRepeatable() {
        return wrappedEntity.isRepeatable();
    }

    @Override
    public boolean isChunked() {
        return wrappedEntity.isChunked();
    }

    @Override
    public long getContentLength() {
        return wrappedEntity.getContentLength();
    }

    @Override
    public Header getContentType() {
        return wrappedEntity.getContentType();
    }

    @Override
    public Header getContentEncoding() {
        return wrappedEntity.getContentEncoding();
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        return wrappedEntity.getContent();
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        CountingOutputStream countingOutputStream = new CountingOutputStream(outputStream, progressConsumer);
        wrappedEntity.writeTo(countingOutputStream);
    }

    @Override
    public boolean isStreaming() {
        return wrappedEntity.isStreaming();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void consumeContent() throws IOException {
        wrappedEntity.consumeContent();
    }
}
