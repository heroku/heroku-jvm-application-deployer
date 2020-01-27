# Changelog

## 3.0.0
* TODO

## Master
* Upgrade to Tomcat Webapp Runner 9.0.30.0

## 2.0.6

* Upgrade to Tomcat Webapp Runner 8.5.33.0
* Update default buildpack URL

## 0.6.0

* Added mechanism to set api key programmatically
* Fix: Symlinks are imported into slug as files, not symlinks

## 0.5.0

* Upgraded to Tomcat 8 as default container

## 0.4.4

* Added a warning for deploy-war when custom processTypes are present

## 0.4.3

* Accounted for Procfile's with empty lines in parsing
* Added ability to define custom buildpacks or multi-buildpack

## 0.4.1

* Added app name detection from Git repo.

## 0.4.0 

* Added ability to log incremental progress of slug upload.

## 0.3.6

* Change config vars to overwrite existing values by default
* Ignore POM packaging if `warFile` config is set

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
