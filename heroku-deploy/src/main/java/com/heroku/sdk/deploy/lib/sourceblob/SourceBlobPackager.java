package com.heroku.sdk.deploy.lib.sourceblob;

import com.heroku.sdk.deploy.lib.OutputAdapter;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SourceBlobPackager {

    public static Path pack(List<SourceBlobContent> includedPaths, OutputAdapter outputAdapter) throws IOException {
        Path tarFilePath = Files.createTempFile("heroku-deploy", "source-blob.tgz");

        TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(
                new GzipCompressorOutputStream(new FileOutputStream(tarFilePath.toFile())));

        tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

        outputAdapter.logInfo("-----> Packaging application...");
        for (SourceBlobContent includedPath : includedPaths) {
            if (includedPath.isHidden()) {
                outputAdapter.logInfo("       - including: " + includedPath.getSourceBlobPath() + " (hidden)");
            } else {
                outputAdapter.logInfo("       - including: " + includedPath.getSourceBlobPath());
            }

            for (SourceBlobContent expandedIncludedPath : expand(includedPath).collect(Collectors.toList())) {
                addIncludedPathToArchive(expandedIncludedPath, tarArchiveOutputStream);
            }
        }

        tarArchiveOutputStream.close();
        return tarFilePath;
    }

    private static Stream<SourceBlobContent> expand(SourceBlobContent includedPath) {
        if (Files.isDirectory(includedPath.getLocalPath())) {
            try {
                return Files.list(includedPath.getLocalPath())
                        .map(subPath -> {
                            Path relativeSubPath = includedPath.getLocalPath().relativize(subPath);
                            Path sourceBlobSubPath = includedPath.getSourceBlobPath().resolve(relativeSubPath);

                            return new SourceBlobContent(subPath, sourceBlobSubPath);
                        })
                        .flatMap(SourceBlobPackager::expand);

            } catch (IOException e) {
                // Ignoring errors while expanding can lead to other errors later in the chain.
                // In order to be able to use Javas native streaming API I made this trade-off deliberately and
                // expect errors being caught downstream.
                return Stream.of(includedPath);
            }
        }

        return Stream.of(includedPath);
    }

    private static void addIncludedPathToArchive(SourceBlobContent includedPath, TarArchiveOutputStream tarArchiveOutputStream) throws IOException {
        // TODO: Symlink handling
        TarArchiveEntry entry = new TarArchiveEntry(includedPath.getLocalPath().toFile(), includedPath.getSourceBlobPath().toString());

        tarArchiveOutputStream.putArchiveEntry(entry);
        Files.copy(includedPath.getLocalPath(), tarArchiveOutputStream);
        tarArchiveOutputStream.closeArchiveEntry();
    }

}
