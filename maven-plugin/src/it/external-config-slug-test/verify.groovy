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
    assert output.contains("1.7"), "Wrong version of JDK packages into slug"

    process = "heroku ps -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("=== web (Free): `java \$JAVA_OPTS -Dmy.var=foobar -cp target/classes:target/dependency/* Main`"), "web process type not detected"

    Thread.sleep(5000)

    process = "curl https://${appName}.herokuapp.com".execute()
    process.waitFor()
    output = process.text
    assert output.contains("Hello from Java!"), "app is not running: ${output}"
} finally {
   ("heroku destroy ${appName} --confirm ${appName}").execute().waitFor();
}