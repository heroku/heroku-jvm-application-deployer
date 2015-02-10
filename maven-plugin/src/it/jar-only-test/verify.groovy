import java.io.*;
import org.codehaus.plexus.util.FileUtils;

def props = new Properties()
new File(basedir, "test.properties").withInputStream {
    stream -> props.load(stream)
}
String appName = props["heroku.appName"]

try {
    def process = "heroku run worker -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("Hello from an executable jar file!"), "Worker did not run: ${output}"
} finally {
   ("heroku destroy " + appName + " --confirm " + appName).execute().waitFor();
}
