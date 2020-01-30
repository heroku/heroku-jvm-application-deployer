package com.heroku.sdk.deploy.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileDownloader {

    public static Path download(URI uri) throws IOException {
        Path temporaryFilePath = Files.createTempFile("heroku-deploy-file-downloader", ".tmp");

        FileOutputStream fileOutputStream = new FileOutputStream(temporaryFilePath.toFile());
        ReadableByteChannel readableByteChannel = Channels.newChannel(uri.toURL().openStream());

        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

        return temporaryFilePath;
    }
}
