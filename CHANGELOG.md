## Master

* Change config vars to overwrite existing values by default

## 0.3.5

* Replaced javax.net with Apache HttpClient
* Improved default JAVA_TOOL_OPTIONS

## 0.3.4

* Upgraded webapp-runner to 7.0.57.2
* Added `<includeTarget>` option to toggle the inclusion of the `target/` directory by default

## 0.3.3

*  Added commit string to slug meta-data

## 0.3.2

*  Included with_jmap and with_jstack scripts.

*  Added `heroku:create-slug` and `heroku:deploy-slug` goals

## 0.3.1

*  Added `heroku:run-war` goal to run a WAR file locally with a command similar on Heroku

*  Added `heroku:dashboard` goal to open the Heroku dashboard for the configured application

*  Began enforcement of `war` packaging when using `-war` goals

*  Added `heroku:eclipse-launch-config` goal to generate Eclipse launch configuration files

*  Switched from custom MojoExecutor to org.twdata.maven:mojo-executor

*  Added caching of the JDK so it won't be downloaded on every deploy

## 0.3.0

*  Jumping to 0.3.x version to align with sbt-heroku

*  [#4] Use explicit versions in pom.xml

*  [#1] Remove support for Java 1.6

## 0.1.9

*  Improved detection of Heroku API key. Now uses .netrc first.
