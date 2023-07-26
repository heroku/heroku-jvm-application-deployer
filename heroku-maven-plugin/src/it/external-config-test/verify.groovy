import java.io.*;
import org.codehaus.plexus.util.FileUtils;

def props = new Properties()
new File(basedir, "test.properties").withInputStream {
    stream -> props.load(stream)
}
String appName = props["heroku.appName"]

try {
    def log = FileUtils.fileRead(new File(basedir, "build.log"));
    assert log.contains("BUILD SUCCESS"), "the build was not successful"

    def process = "heroku run java -version -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("1.7"), "Wrong version of JDK packages into slug"

    process = "heroku ps -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("java \$JAVA_OPTS -Dmy.var=foobar -cp target/classes:target/dependency/* Main"), "web process type not detected"

    Thread.sleep(5000)

    def appInfoCommand = ("heroku apps:info -a " + appName + " --json").execute()
    appInfoCommand.waitFor()

    String appWebUrl = new groovy.json.JsonSlurper().parseText(appInfoCommand.text).app.web_url

    process = "curl ${appWebUrl}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("Hello from Java!"), "app is not running: ${output}"
} finally {
   ("heroku destroy ${appName} --confirm ${appName}").execute().waitFor();
}
