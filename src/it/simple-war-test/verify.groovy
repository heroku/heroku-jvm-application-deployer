import java.io.*;
import org.codehaus.plexus.util.FileUtils;

try {
    String log = FileUtils.fileRead(new File(basedir, "build.log"));
    String expected = "BUILD SUCCESS";

    if (!log.contains(expected)) {
        assert failure, "log file does not contain '" + expected + "'"
    }
} finally {
    def props = new Properties()
    new File(basedir, "test.properties").withInputStream {
        stream -> props.load(stream)
    }
    String appName = props["heroku.appName"]

   ("heroku destroy " + appName + " --confirm " + appName).execute().waitFor();
}