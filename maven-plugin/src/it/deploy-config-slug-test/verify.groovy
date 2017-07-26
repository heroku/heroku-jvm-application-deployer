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

    Thread.sleep(3000)
    def process = "heroku run cat test.txt -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("It worked!"), "slug did not contain test file: ${output}"

    Thread.sleep(3000)
    process = "heroku run cat public/page.html -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("<html></html>"), "Include wildcards did not work: ${output}"

    Thread.sleep(3000)
    process = "heroku run cat public/javascripts/hello.js -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("Welcome to your Play application's JavaScript!"), "slug did not contain js file: ${output}"

    process = "heroku config -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("MY_VAR"), "config var MY_VAR was not present"
    assert output.contains("SomeValue"), "config var MY_VAR has the wrong value"

    Thread.sleep(3000)
    process = "heroku run java -version -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("1.7"), "Wrong version of JDK packages into slug"

    Thread.sleep(3000)
    process = "heroku run cat .jdk/jre/lib/security/test.txt -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("hello from security"), "JDK Overlay not copied"

    process = "heroku stack -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("* cedar-14"), "Wrong stack used: ${output}" // slug deploy defaults to cedar-14

    process = "curl https://${appName}.herokuapp.com".execute()
    process.waitFor()
    output = process.text
    assert output.contains("Hello from Java!"), "app is not running: ${output}"
} finally {
   ("heroku destroy ${appName} --confirm ${appName}").execute().waitFor();
}