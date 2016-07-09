import java.io.*;
import org.codehaus.plexus.util.FileUtils;

def props = new Properties()
new File(basedir, "test.properties").withInputStream {
    stream -> props.load(stream)
}
String appName = props["heroku.appName"]

try {
    def log = FileUtils.fileRead(new File(basedir, "build.log"));
    assert log.contains("Creating slug"), "did not create the slug"
    assert log.contains("Uploading slug"), "did not upload the slug"
    assert log.contains("BUILD SUCCESS"), "the build was not successful"

    def process = "heroku run java -version -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("1.8"), "Wrong version of JDK packages into slug"

    Thread.sleep(5000)

    process = "heroku logs -n 2000 -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("--expand-war"), "Did not pick up WEBAPP_RUNNER_OPTS: ${output}"

    process = "curl https://${appName}.herokuapp.com".execute()
    process.waitFor()
    output = process.text
    if (!output.contains("Hello from Java!")) {
        throw new RuntimeException("app is not running: ${output}")
    }
} finally {
   ("heroku destroy " + appName + " --confirm " + appName).execute().waitFor();
}