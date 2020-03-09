package com.heroku.sdk.deploy.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

public class CountingOutputStream extends OutputStream {
    private OutputStream wrappedOutputStream;
    private Consumer<Long> progressConsumer;
    private long bytesWritten = 0L;

    public CountingOutputStream(OutputStream wrappedOutputStream, Consumer<Long> progressConsumer) {
        this.wrappedOutputStream = wrappedOutputStream;
        this.progressConsumer = progressConsumer;
    }

    @Override
    public void write(int i) throws IOException {
        wrappedOutputStream.write(i);
        bytesWritten++;
        progressConsumer.accept(bytesWritten);
    }

    @Override
    public void write(byte[] b) throws IOException {
        wrappedOutputStream.write(b);
        bytesWritten += b.length;
        progressConsumer.accept(bytesWritten);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        wrappedOutputStream.write(b, off, len);
        bytesWritten += len;
        progressConsumer.accept(bytesWritten);
    }

    @Override
    public void flush() throws IOException {
        wrappedOutputStream.flush();
    }

    @Override
    public void close() throws IOException {
        wrappedOutputStream.close();
    }

    public long getBytesWritten() {
        return bytesWritten;
    }
}
