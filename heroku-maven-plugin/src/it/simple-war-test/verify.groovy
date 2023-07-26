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
    assert log.contains("heroku-maven-plugin app detected"), "jvm-common buildpack not detected"
    assert log.contains("BUILD SUCCESS"), "the build was not successful"

    Thread.sleep(3000)
    def process = "heroku run java -version -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("1.8"), "Wrong version of JDK packages into slug: '${output}'"

    Thread.sleep(3000)
    process = "heroku run cat target/mvn-dependency-list.log -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("The following files have been resolved"), "Dependencies not listed: '${output}'"

    Thread.sleep(3000)
    process = "heroku run echo \$WEBAPP_RUNNER_OPTS -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("--expand-war"), "WEBAPP_RUNNER_OPTS not correctly set: '${output}'"

    Thread.sleep(3000)
    process = "heroku run cat Procfile -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("\$WEBAPP_RUNNER_OPTS"), "WEBAPP_RUNNER_OPTS not present in Procfile: '${output}'"

    Thread.sleep(3000)
    def appInfoCommand = ("heroku apps:info -a " + appName + " --json").execute()
    appInfoCommand.waitFor()

    String appWebUrl = new groovy.json.JsonSlurper().parseText(appInfoCommand.text).app.web_url

    process = "curl ${appWebUrl}".execute()
    process.waitFor()
    output = process.text
    if (!output.contains("Hello from Java!")) {
        throw new RuntimeException("app is not running: '${output}'")
    }
} finally {
   ("heroku destroy " + appName + " --confirm " + appName).execute().waitFor();
}
