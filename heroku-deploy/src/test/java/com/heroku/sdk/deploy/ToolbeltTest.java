package com.heroku.sdk.deploy;

import org.eclipse.jgit.transport.NetRC;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ToolbeltTest {

  @Test
  public void testReadNetrcFile() throws IOException {
    String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
    String netrcFile = relPath + "../../src/test/resources/sample.netrc";

    NetRC netrc = Toolbelt.readNetrcFile(new File(netrcFile));

    assertEquals(2, netrc.getEntries().size());
    assertEquals("01234567-89ab-cdef-0123-456789abcdef", String.valueOf(netrc.getEntry("one.example.com").password));
    assertEquals("01234567-89ab-cdef-0123-456789ghijk", String.valueOf(netrc.getEntry("three.example.com").password));
  }
}
