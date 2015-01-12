# heroku-maven-plugin [![Build Status](https://travis-ci.org/heroku/heroku-maven-plugin.svg)](https://travis-ci.org/heroku/heroku-maven-plugin)

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
      <version>0.3.0</version>
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
      <version>0.3.0</version>
      <configuration>
        <appName>${heroku.appName}</appName>
      </configuration>
    </plugin>
  </plugins>
</build>
```

This assumes your project will generate a WAR file in the `target` directory. If the WAR file is located somewhere else,
you can specify this with the `<warFile>` configuration element. The `<processTypes>` element is not needed because
the plugin will determine the appropriate process type for you.

Now, if you have the [Heroku Toolbelt](https://toolbelt.heroku.com/) installed, run:

```
$ mvn heroku:deploy-war
```

If you do not have the toolbelt installed or have not logged in, then run:

```
$ HEROKU_API_KEY="xxx-xxx-xxxx" mvn heroku:deploy-war
```

And replace "xxx-xxx-xxxx" with the value of your Heroku API token.

## Creating a slug-file

If you have configured the plugin as described before you can use it to create the slug-file in a seperate step by calling

```
$ mvn heroku:create-slug
```

After that you can deploy your slug via the above mentioned

```$ mvn heroku:deploy``` or ```$ mvn heroku:deploy-war```

If no slug-file exists when you call the deploy goals a slug-file is created when needed.

## Requirements

+  Maven 3.2.x
+  Java 1.7 or higher

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

You can include additional directories in the slug as long as they are relative to the project root:

```
<includes>
  <include>etc/readme.txt</include>
</includes>
```

You can set the Heroku runtime stack like this:

```
<stack>cedar-14</stack>
```

See the integration tests under `maven-plugin/src/it` for more examples.

## Deploying to Multiple Apps

In most real-world scenarios, you will need to deploy your application to dev, test and prod environments.
There are several ways of handling this.

### Using Maven Profiles

Use a profile for each app, and configure the plugin accordingly. For example:

```
<build>
  <plugins>
    <plugin>
      <groupId>com.heroku.sdk</groupId>
      <artifactId>heroku-maven-plugin</artifactId>
      <version>${heroku-maven-plugin.version}</version>
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
            <!-- <slugFileName> not set will result in slug.tgz -->
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
            <slugFileName>myapp-prod.tgz</slugFileName>
          </configuration>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

### Using System Properties

You can provide the application name as a system property like this:

```
$ mvn heroku:deploy -Dheroku.appName=myapp
```

### Using a Heroku Properties File

This solution is best when multiple developers each need their own apps.
Create a `heroku.properties` file in the root directory of your application and put the following code in it
(but replace "myapp" with the name of your Heroku application):

```
heroku.appName=myapp
```

Then add the file to your `.gitignore` so that each developer can have their own local versions of the file.
The value in `heroku.properties` will take precedence over anything configured in your  `pom.xml`.

## Hacking

To run the entire suite of integration tests, use the following command:

```
$ mvn clean install -Pintegration-test
```

To run an individual integration test, use a command like this:

```
$ mvn clean install -Pintegration-test -Dinvoker.test=simple-deploy-test
```
