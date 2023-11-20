package com.heroku.deployer.sourceblob;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SourceBlobPackager {

    public static Path pack(SourceBlobDescriptor sourceBlobDescriptor) throws IOException {
        Path tarFilePath = Files.createTempFile("heroku-deploy", "source-blob.tgz");

        TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(
                new GzipCompressorOutputStream(new FileOutputStream(tarFilePath.toFile())));

        tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

        System.out.println("-----> Packaging application...");
        for (Path sourceBlobPath : sourceBlobDescriptor.getContents().keySet())  {
            SourceBlobDescriptor.SourceBlobContent content = sourceBlobDescriptor.getContents().get(sourceBlobPath);

            if (content.isHidden()) {
                System.out.println("       - including: " + sourceBlobPath + " (hidden)");
            } else {
                System.out.println("       - including: " + sourceBlobPath);
            }

            addIncludedPathToArchive(sourceBlobPath.toString(), content, tarArchiveOutputStream);
        }

        tarArchiveOutputStream.close();

        System.out.println("-----> Creating build...");
        System.out.println("       - file: " + tarFilePath);
        System.out.println(String.format("       - size: %dMB", Files.size(tarFilePath) / 1024 / 1024));

        return tarFilePath;
    }

    private static void addIncludedPathToArchive(String path, SourceBlobDescriptor.SourceBlobContent content, TarArchiveOutputStream tarArchiveOutputStream) throws IOException {
        if (content.isLocalPath()) {
            TarArchiveEntry entry = new TarArchiveEntry(content.getLocalPath().toFile(), path);

            if (Files.isSymbolicLink(content.getLocalPath())) {
                Path symbolicLink = Files.readSymbolicLink(content.getLocalPath());
                entry.setLinkName(symbolicLink.toString());

                tarArchiveOutputStream.putArchiveEntry(entry);
                tarArchiveOutputStream.closeArchiveEntry();

            } else {
                if (Files.isExecutable(content.getLocalPath())) {
                    entry.setMode(493);
                }

                tarArchiveOutputStream.putArchiveEntry(entry);
                Files.copy(content.getLocalPath(), tarArchiveOutputStream);
                tarArchiveOutputStream.closeArchiveEntry();

            }
        } else {
            TarArchiveEntry entry = new TarArchiveEntry(path);
            entry.setSize(content.getSyntheticFileContents().getBytes(StandardCharsets.UTF_8).length);

            tarArchiveOutputStream.putArchiveEntry(entry);

            OutputStreamWriter writer = new OutputStreamWriter(tarArchiveOutputStream, StandardCharsets.UTF_8);
            writer.write(content.getSyntheticFileContents());
            writer.flush();

            tarArchiveOutputStream.closeArchiveEntry();
        }
    }
}
