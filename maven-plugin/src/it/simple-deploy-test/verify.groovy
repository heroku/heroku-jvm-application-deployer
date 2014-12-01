import java.io.*;
import org.codehaus.plexus.util.FileUtils;

def props = new Properties()
new File(basedir, "test.properties").withInputStream {
    stream -> props.load(stream)
}
String appName = props["heroku.appName"]

try {
    def log = FileUtils.fileRead(new File(basedir, "build.log"));
    if (!log.contains("BUILD SUCCESS")) {
        throw new RuntimeException("the build was not successful")
    }

    def process = "heroku run java -version -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("1.8"), "Wrong version of JDK packages into slug"

    Thread.sleep(7000)

    process = "heroku logs -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("Picked up JAVA_TOOL_OPTIONS: -Xmx384m"), "profile.d script was not run: " + output

    process = "curl https://${appName}.herokuapp.com".execute()
    process.waitFor()
    output = process.text
    if (!output.contains("Hello from Java!")) {
        throw new RuntimeException("app is not running: ${output}")
    }
} finally {
   ("heroku destroy " + appName + " --confirm " + appName).execute().waitFor();
}