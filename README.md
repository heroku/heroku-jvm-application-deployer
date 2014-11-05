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
      <groupId>com.heroku.maven</groupId>
      <artifactId>heroku-maven-plugin</artifactId>
      <version>1.0-SNAPSHOT</version>
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
      <groupId>com.heroku.maven</groupId>
      <artifactId>heroku-maven-plugin</artifactId>
      <version>1.0-SNAPSHOT</version>
      <configuration>
        <appName>${heroku.appName}</appName>
      </configuration>
    </plugin>
  </plugins>
</build>
```

This assumes your project will generate a WAR file in the `target` directory. If the WAR file is located somewhere else,
you can specify this with the `<warFile>` configuration element.

Now, if you have the [Heroku Toolbelt](https://toolbelt.heroku.com/) installed, run:

```
$ mvn heroku:deploy-war
```

If you do not have the toolbelt installed, then run:

```
$ HEROKU_API_KEY="xxx-xxx-xxxx" mvn heroku:deploy
```

And replace "xxx-xxx-xxxx" with the value of your Heroku API token.


## Hacking

To run the entire suite of integration tests, use the following command:

```
$ mvn clean install -Pintegration-test
```

To run an individual integration test, use a command like this:

```
$ mvn clean install -Pintegration-test -Dinvoker.test=simple-deploy-test
```

