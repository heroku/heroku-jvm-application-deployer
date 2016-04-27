import java.io.*;
import org.codehaus.plexus.util.FileUtils;

def props = new Properties()
new File(basedir, "test.properties").withInputStream {
    stream -> props.load(stream)
}
String appName = props["heroku.appName"]

try {
    def log = FileUtils.fileRead(new File(basedir, "build.log"));
    assert log.contains("Creating build"), "the build was not created"
    assert log.contains("Uploading build"), "the build was not uploaded"
    assert log.contains("remote:"), "the buildpacks were not run"
    assert log.contains("Null app detected"), "null buildpack not detected"
    assert log.contains("heroku-maven-plugin app detected"), "jvm-common buildpack not detected"
    assert log.contains("BUILD SUCCESS"), "the build was not successful"

    Thread.sleep(4000)
    def process = "heroku run java -version -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("1.8"), "Wrong version of JDK packaged into slug: ${output}"

    Thread.sleep(4000)
    process = "heroku run with_jstack java -version -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("1.8"), "with_jstack failed: ${output}"

    Thread.sleep(4000)
    process = "heroku run with_jmap java -version -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("1.8"), "with_jmap failed: ${output}"

    process = "curl https://${appName}.herokuapp.com".execute()
    process.waitFor()
    output = process.text
    if (!output.contains("Hello from Java!")) {
        throw new RuntimeException("app is not running: ${output}")
    }
} finally {
   ("heroku destroy " + appName + " --confirm " + appName).execute().waitFor();
}
