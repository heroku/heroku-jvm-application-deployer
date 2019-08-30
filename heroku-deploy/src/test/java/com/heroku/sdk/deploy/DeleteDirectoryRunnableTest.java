// $Id\$
package com.heroku.sdk.deploy;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Kaiser 08/30/19
 */
public class DeleteDirectoryRunnableTest {

    @Test
    public void testRecursiveDirectoryDeletion() throws Exception {
        final File tempDir = Files.createTempDirectory("DeleteDirectoryRunnableTest_").toFile();
        assertTrue(tempDir.exists());
        assertTrue(tempDir.isDirectory());

        createFiles(tempDir);

        final DeleteDirectoryRunnable runnable = new DeleteDirectoryRunnable(tempDir.toPath());
        Thread t = new Thread(runnable);

        t.start();

        t.join(3000);
        assertFalse(tempDir.exists());
    }

    private void createFiles(File tempDir) throws IOException {
        File f1 = new File(tempDir,"f1");
        f1.createNewFile();

        File f2 = new File(tempDir,"f2");
        f2.createNewFile();

        File d1 = new File(tempDir,"d1");
        d1.mkdir();

        File d11 = new File(d1,"d11");
        d11.mkdir();

        File f111 = new File(d11,"f111");
        f111.mkdir();

        File f11 = new File(d1,"f11");
        f11.mkdir();
    }

}
