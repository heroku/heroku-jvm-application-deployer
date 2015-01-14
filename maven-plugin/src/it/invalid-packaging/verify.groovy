import java.io.*;
import org.codehaus.plexus.util.FileUtils;

def props = new Properties()
new File(basedir, "test.properties").withInputStream {
    stream -> props.load(stream)
}
String appName = props["heroku.appName"]

try {
    def log = FileUtils.fileRead(new File(basedir, "build.log"));
    assert log.contains("BUILD FAILURE"), "the build was successful but should NOT have been!"
    assert log.contains("Your packaging must be set to 'war' to use this goal!"), "the build was successful but should NOT have been!"
} finally {
   ("heroku destroy " + appName + " --confirm " + appName).execute().waitFor();
}