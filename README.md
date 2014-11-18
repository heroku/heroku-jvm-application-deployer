# heroku-maven-plugin

This plugin is used to deploy Java applications directly to Heroku without pushing to a Git repository.
This is can be useful when deploying from a CI server.

The plugin has two targets:

+  `heroku:deploy` for deploying standalone applications

+  `heroku:deploy-war` for deploying WAR files

## Deploying Standalone Applications

Add the following to your `pom.xml`, but replace the `<web>` element with the command used to run your application.

```
<build>
  <plugins>
    <plugin>
      <groupId>com.heroku.sdk</groupId>
      <artifactId>heroku-maven-plugin</artifactId>
      <version>0.1.4</version>
      <configuration>
        <appName>${heroku.appName}</appName>
        <processTypes>
          <web>java $JAVA_OPTS -cp target/classes:target/dependency/* Main</web>
        </processTypes>
      </configuration>
    </plugin>
  </plugins>
</build>
```

Now, if you have the [Heroku Toolbelt](https://toolbelt.heroku.com/) installed, run:

```
$ mvn heroku:deploy
```

If you do not have the toolbelt installed, then run:

```
$ HEROKU_API_KEY="xxx-xxx-xxxx" mvn heroku:deploy
```

And replace "xxx-xxx-xxxx" with the value of your Heroku API token.

## Deploying WAR Files

Add the following to your `pom.xml`, but replace the `<web>` element with the command used to run your application.

```
<build>
  <plugins>
    <plugin>
      <groupId>com.heroku.sdk</groupId>
      <artifactId>heroku-maven-plugin</artifactId>
      <version>0.1.4</version>
      <configuration>
        <appName>${heroku.appName}</appName>
      </configuration>
    </plugin>
  </plugins>
</build>
```

This assumes your project will generate a WAR file in the `target` directory. If the WAR file is located somewhere else,
you can specify this with the `<warFile>` configuration element. The plugin will determine the appropriate process type
for you, but you can override this with the `<processTypes>` configuration element.

Now, if you have the [Heroku Toolbelt](https://toolbelt.heroku.com/) installed, run:

```
$ mvn heroku:deploy-war
```

If you have not logged in with the toolbelt (by running `heroku auth:login`) then the process will hang.

If you do not have the toolbelt installed or have not logged in, then run:

```
$ HEROKU_API_KEY="xxx-xxx-xxxx" mvn heroku:deploy-war
```

And replace "xxx-xxx-xxxx" with the value of your Heroku API token.

## Requirements

+  Maven 3.2.x

## Configuration

In the `<configuration>` element of the plugin, you can set the JDK version like so:

```
<jdkVersion>1.8</jdkVersion>
```

The default is 1.8, but 1.6 and 1.7 are valid values. The plugin will also pick up the `java.runtime.version` set in
your `system.properties` file, but the plugin configuration will take precedence. 

You can set configuration variables like this:

```
<configVars>
  <MY_VAR>SomeValue</MY_VAR>
  <JAVA_OPTS>-Xss512k -XX:+UseCompressedOops</JAVA_OPTS>
</configVars>
```

Any variable defined in `configVars` will override defaults and previous defined config variables.

You may set process types (similar to a `Procfile`):

```
<processTypes>
  <web>java $JAVA_OPTS -cp target/classes:target/dependency/* Main</web>
  <worker>java $JAVA_OPTS -cp target/classes:target/dependency/* Worker</worker>
</processTypes>
```

The plugin will also pick up any process types defined in your `Procfile`, but the plugin configuration 
will take precedence.

Finally, you can include additional directories in the slug as long as they are relative to the project root:

```
<includes>
  <include>etc/readme.txt</include>
</includes>
```

See the integration tests under `maven-plugin/src/it` for more examples.

## Deploying to Multiple Apps

In most real-world scenarios, you will need to deploy your application to dev, test and prod environments. This is best 
handled with Maven profiles. For example:

```
<build>
  <plugins>
    <plugin>
      <groupId>com.heroku.sdk</groupId>
      <artifactId>heroku-maven-plugin</artifactId>
      <version>0.1.0</version>
      <configuration>
        <processTypes>
          <web>java $JAVA_OPTS -cp target/classes:target/dependency/* Main</web>
        </processTypes>
      </configuration>
    </plugin>
  </plugins>
</build>
<profiles>
  <profile>
    <id>test</id>
    <build>
      <plugins>
        <plugin>
          <groupId>com.heroku.sdk</groupId>
          <artifactId>heroku-maven-plugin</artifactId>
          <configuration>
            <appName>myapp-test</appName>
          </configuration>
        </plugin>
      </plugins>
    </build>
  </profile>
  <profile>
    <id>prod</id>
    <build>
      <plugins>
        <plugin>
          <groupId>com.heroku.sdk</groupId>
          <artifactId>heroku-maven-plugin</artifactId>
          <configuration>
            <appName>myapp-prod</appName>
          </configuration>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

Alternatively, you can provide the application name as a system property like this:

```
$ mvn heroku:deploy -Dheroku.appName=myapp
```

## Hacking

To run the entire suite of integration tests, use the following command:

```
$ mvn clean install -Pintegration-test
```

To run an individual integration test, use a command like this:

```
$ mvn clean install -Pintegration-test -Dinvoker.test=simple-deploy-test
```

