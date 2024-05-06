package com.heroku.deployer.deployment;

import org.apache.commons.io.FileUtils;
import java.util.function.BiConsumer;

public final class UploadProgressReporter implements BiConsumer<Long, Long> {
    private long lastOutput;
    private long lastBytes;

    @Override
    public void accept(Long currentBytes, Long totalBytes) {
        if (System.currentTimeMillis() - lastOutput > 1000) {
            System.out.printf("       Uploaded %s/%s (%.1f%%, %s/s)\n",
                    FileUtils.byteCountToDisplaySize(currentBytes),
                    FileUtils.byteCountToDisplaySize(totalBytes),
                    ((float) currentBytes / (float) totalBytes) * 100.0,
                    FileUtils.byteCountToDisplaySize(currentBytes - lastBytes)
            );

            lastOutput = System.currentTimeMillis();
            lastBytes = currentBytes;
        }
    }
}
