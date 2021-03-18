# heroku-maven-plugin [![Build Status](https://travis-ci.org/heroku/heroku-maven-plugin.svg?branch=main)](https://travis-ci.org/heroku/heroku-maven-plugin) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.heroku.sdk/heroku-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.heroku.sdk/heroku-maven-plugin)

This plugin is used to deploy Java applications directly to Heroku without pushing to a Git repository. It uses 
[Heroku's Platform API](https://devcenter.heroku.com/articles/platform-api-quickstart). 
This is can be useful when deploying from a CI server, deploying pre-built JAR or WAR files.

The plugin has two main goals:

- `heroku:deploy` and `heroku:deploy-only` for deploying standalone applications
- `heroku:deploy-war` for deploying WAR files

In addition to those two main goals, three additional goals are available:

- `heroku:dashboard` to open the Heroku dashboard for the configured application
- `heroku:run-war` to locally running WAR files
- `heroku:eclipse-launch-config` to generate launch configurations for Eclipse IDE

## Requirements

- Maven 3.5.x
- Java 8u101 or higher (versions < u101 might experience difficulties displaying build log output)

## Table of Contents
* [Global Configuration](#global-configuration)
* [Cookbook](#cookbook)
    * [Deploying a Standalone Application](#deploying-a-standalone-application)
    * [Deploying a WAR File](#deploying-a-war-file)
    * [Running a WAR File Locally](#running-a-war-file-locally)
    * [Deploying to Multiple Apps](#deploying-to-multiple-apps) (i.e.: staging/production setups)
* [Plugin Configuration](#plugin-configuration)
    * [appName](#appname)
    * [jdkVersion](#jdkversion)
    * [configVars](#configvars)
    * [includes](#includes)
    * [includeTarget](#includetarget)
    * [logProgress](#logprogress)
    * [buildpacks](#buildpacks)
    * [processTypes](#processtypes)
    * [warFile](#warfile)
    * [webappRunnerVersion](#webapprunnerversion)
* [Advanced Features](#advanced-features)
* [Hacking](#hacking)

## Global Configuration

### Heroku API Key
This plugin uses Heroku's Platform API and thus requires an API key to function. If you have the 
[Heroku CLI](https://cli.heroku.com/) installed and logged in with `heroku login`, the plugin will automatically
pick up your API key. Alternatively, you can use the `HEROKU_API_KEY` environment variable to set your API key:

```sh-session
$ HEROKU_API_KEY="xxx-xxx-xxxx" mvn heroku:deploy
```

## Cookbook

### Deploying a Standalone Application

Add the following to your `pom.xml`, but replace the `<web>` element with the command used to run your application.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.heroku.sdk</groupId>
      <artifactId>heroku-maven-plugin</artifactId>
      <version>3.0.4</version>
      <configuration>
        <appName>${heroku.appName}</appName>
        <processTypes>
          <web>java $JAVA_OPTS -cp target/classes:target/dependency/* com.example.Main</web>
        </processTypes>
      </configuration>
    </plugin>
  </plugins>
</build>
```

You can then run the following command to deploy your application:

```sh-session
$ mvn heroku:deploy
```

### Deploying a WAR File

NOTE: This requires that you use `<packaging>war</packaging>` in your `pom.xml`.

Add the following to your `pom.xml`.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.heroku.sdk</groupId>
      <artifactId>heroku-maven-plugin</artifactId>
      <version>3.0.4</version>
      <configuration>
        <appName>${heroku.appName}</appName>
      </configuration>
    </plugin>
  </plugins>
</build>
```

This assumes your project will generate a WAR file in the `target` directory. If the WAR file is located somewhere else,
you can specify this with the `<warFile>` configuration element. The `<processTypes>` element is not needed
and *will be ignored* because the plugin will determine the appropriate process type for you.

You can then run the following command to deploy your application:

```sh-session
$ mvn heroku:deploy-war
```

### Running a WAR File Locally

You can execute your WAR file locally by running the following command:

```sh-session
$ mvn heroku:run-war
```

This will start the web application in a way that is very similar to how it is run on Heroku. If you need more control
over how the WAR file is being run, you can use [webapp-runner](https://github.com/heroku/webapp-runner) directly.

### Deploying to Multiple Apps

In most real-world scenarios, you will need to deploy your application to dev, test and prod environments.
There are several ways of handling this.

#### Using Maven Profiles

Use a profile for each app, and configure the plugin accordingly. For example:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.heroku.sdk</groupId>
      <artifactId>heroku-maven-plugin</artifactId>
      <version>3.0.4</version>
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

#### Using System Properties

You can provide the application name as a system property like this:

```sh-session
$ mvn heroku:deploy -Dheroku.appName=myapp
```

#### Using a Heroku Properties File

This solution is best when multiple developers each need their own apps.
Create a `heroku.properties` file in the root directory of your application and put the following code in it
(but replace "myapp" with the name of your Heroku application):

```
heroku.appName=myapp
```

Then add the file to your `.gitignore` so that each developer can have their own local versions of the file.
The value in `heroku.properties` will take precedence over anything configured in your  `pom.xml`.

## Plugin Configuration
### appName

The name of the application to deploy.

```xml
<appName>my-app-name</appName>
```

The plugin will detect the appName from the following places in this order:

* The `heroku.properties` file
* The `heroku.appName` system property
* The Maven configuration (shown above)
* The "heroku" Git remote

For example, if you specify the `heroku.appName` system property, the Maven configuration shown above will have
no effect. This is useful for [deploying multiple apps](#deploying-to-multiple-apps).

### buildVersion

The build version of the application to deploy.

```xml
<buildVersion>v1.0.0</buildVersion>
```

The plugin will detect the buildVersion from the following places in this order:

* The `heroku.buildVersion` system property
* The Maven configuration (shown above)
* The Maven project version (`${project.version}`)

### jdkVersion

The JDK version to use for your application.

```xml
<jdkVersion>11</jdkVersion>
```

The plugin will look for the required JDK version in the following places, in order:

* The Maven configuration (shown above)
* The `heroku.jdkVersion` system property
* The `java.runtime.version` in `system.properties` located in the root directory of your project 

For valid values and the current default, refer to [Heroku's DevCenter article about specifing a Java version](https://devcenter.heroku.com/articles/java-support#specifying-a-java-version).

### configVars

Sets configuration variables for the application on each deploy.

```xml
<configVars>
  <MY_VAR>SomeValue</MY_VAR>
  <JAVA_OPTS>-Xss512k -XX:+UseCompressedOops</JAVA_OPTS>
</configVars>
```

Any variable defined in `configVars` will override defaults and previously defined config variables.

Note: If you adhere to the principles of the 12 Factor app, configuration should be strictly separated from code. 
Thus, you do not want to tie your configuration to your codebase. There are a few exceptions to this, like some JAVA_OPTS may be universal.
If possible, you should not use `configVars` at all.

### includes
Allows you to include additional files and directories.

```xml
<includes>
  <include>etc/readme.txt</include>
</includes>
```

Included files and directories must be located within your project root directory.

### includeTarget
Allows you to disable automatic inclusion of the `target` directory. (Default is `true`)

```xml
<includeTarget>false</includeTarget>
```

This is useful in cases where you build a single fat JAR with all dependencies you want to deploy. 

### logProgress

Enables or disables logging of the source blob upload progress. (Default is `false`)

```xml
<logProgress>true</logProgress>
```

Enabling this will log the upload progress status at `DEBUG` level.

### buildpacks

Defines the buildpacks used by your application.

```xml
<buildpacks>
  <buildpack>https://github.com/DuckyTeam/heroku-buildpack-imagemagick</buildpack>
  <buildpack>heroku/jvm</buildpack>
</buildpacks>
```

It can sometimes be useful to use more than just the default `heroku/jvm` buildpack for your application. In the above
example, you can see how an additional buildpack is used to install imagemagick.

You can also define your buildpacks using the Heroku Dashboard or the Heroku CLI (i.e. the `heroku buildpacks` command).

### processTypes

Adds process types to the generated [Procfile](https://devcenter.heroku.com/articles/procfile) for your application.

```xml
<processTypes>
  <web>java $JAVA_OPTS -cp target/classes:target/dependency/* Main</web>
  <worker>java $JAVA_OPTS -cp target/classes:target/dependency/* Worker</worker>
</processTypes>
```

The plugin will generate a `Procfile` for your application and include it. You can use this configuration to add more
process types. It will also pick up entries in the `Procfile` within your project root directory, but the plugin configuration
will take precedence.

### warFile
Specifies the WAR file to use for `heroku:deploy-war` and `heroku:run-war` goals.

```xml
<warFile>custom/directory/webapp.war</warFile>
```

Normally, the plugin looks for a WAR file in `target` and deploys it. This means you usually don't have to configure
this, unless you want to deploy a WAR file located somewhere else.

### webappRunnerVersion

Configures the version of [webapp-runner](https://github.com/heroku/webapp-runner) to use for `heroku:deploy-war` and `heroku:run-war`.

```xml
<webappRunnerVersion>9.0.30.0</webappRunnerVersion>
```
## Advanced Features

### Customizing the JDK
You can customize the JDK by creating a `.jdk-overlay` directory as described in [this Dev Center article](https://devcenter.heroku.com/articles/customizing-the-jdk).
This plugin will automatically include this directory if present.

## Hacking

To run the entire suite of integration tests, use the following command:

```sh-session
$ ./mvnw clean install -Pit
```

To run an individual integration test, use a command like this:

```sh-session
$ ./mvnw clean install -Pit -Dinvoker.test=simple-deploy-test
```
