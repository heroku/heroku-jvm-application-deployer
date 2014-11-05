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

    def process = "heroku run cat test.txt -a${appName}".execute()
    process.waitFor()
    output = process.text
    if (!output.contains("It worked!")) {
        throw new RuntimeException("slug did not contain test file: ${output}")
    }

    process = "curl https://${appName}.herokuapp.com".execute()
    process.waitFor()
    output = process.text
    if (!output.contains("Hello from Java!")) {
        throw new RuntimeException("app is not running: ${output}")
    }
} finally {
   ("heroku destroy ${appName} --confirm ${appName}").execute().waitFor();
}