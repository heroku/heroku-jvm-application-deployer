import java.io.*;
import org.codehaus.plexus.util.FileUtils;

def props = new Properties()
new File(basedir, "test.properties").withInputStream {
    stream -> props.load(stream)
}
String appName = props["heroku.appName"]

try {
    def process = "heroku run java -version -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("1.8"), "Wrong version of JDK packaged into slug: ${output}"

    process = "heroku run cat .startup/with_jmap -a${appName}".execute()
    process.waitFor()
    output = process.text
    process = "curl -L https://raw.githubusercontent.com/heroku/heroku-buildpack-jvm-common/master/opt/with_jmap".execute()
    process.waitFor()
    jvmCommonScript = process.text
    assert output.contains(jvmCommonScript), "with_jmap script not copied properly: ${output}"

    process = "heroku run cat .startup/with_jstack -a${appName}".execute()
    process.waitFor()
    output = process.text
    process = "curl -L https://raw.githubusercontent.com/heroku/heroku-buildpack-jvm-common/master/opt/with_jstack".execute()
    process.waitFor()
    jvmCommonScript = process.text
    assert output.contains(jvmCommonScript), "with_jstack script not copied properly: ${output}"

    process = "heroku run with_jstack java -version -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("1.8"), "with_jstack failed: ${output}"

    process = "heroku run with_jmap java -version -a${appName}".execute()
    process.waitFor()
    output = process.text
    assert output.contains("1.8"), "with_jmap failed: ${output}"

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
