# Heroku JVM Application Deployer &emsp; ![License](https://img.shields.io/github/license/heroku/heroku-maven-plugin) ![Maven Central](https://img.shields.io/maven-central/v/com.heroku.sdk/heroku-sdk-parent) [![CI](https://github.com/heroku/heroku-maven-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/heroku/heroku-maven-plugin/actions/workflows/ci.yml)

Command line tool to deploy JVM applications directly to Heroku without pushing to a Git repository. It uses 
[Heroku's Platform API](https://devcenter.heroku.com/articles/platform-api-quickstart). This can be useful when deploying from a CI server, deploying pre-built JAR or WAR files.

It will automatically include and configure a Tomcat server via [webapp-runner](https://github.com/heroku/webapp-runner) if a WAR file is deployed.

> [!IMPORTANT]
> This repository previously contained Heroku's Maven Plugin for deploying JVM applications. This plugin will be sunset
> soon. Customers currently using the Maven Plugin can then use the new Heroku JVM application deployer CLI to deploy
> JAR and WAR files to Heroku without using Git.
>
> The code of the last Heroku Maven plugin release can be found here:
> https://github.com/heroku/heroku-maven-plugin/tree/3.0.7

## Table of Contents
* [Installation](#installation)
* [Usage](#usage)
* [Requirements](#requirements)
* [Configuration](#configuration)
  + [Heroku API Key](#heroku-api-key)

## Installation

Download the latest release from [Maven Central](https://repo1.maven.org/maven2/com/heroku/sdk/heroku-deploy-standalone/).
The JAR file contains all required dependencies.

## Usage

```shell
java -jar heroku-deploy-standalone-0.0.0.jar --help
```

```
Usage: heroku-deploy-standalone [-hV] [--disable-auto-includes] [-a=<appName>]
                                [-j=<jdkString>] [--jar-opts=<jarFileOpts>]
                                [--webapp-runner-version=<webappRunnerVersion>]
                                [-b[=<buildpacks>...]]... [-i
                                [=<includedPaths>...]]... <mainFile>
Application for deploying Java applications to Heroku.
      <mainFile>          The JAR or WAR file to deploy.
  -a, --app=<appName>     The name of the Heroku app to deploy to.
  -b, --buildpack[=<buildpacks>...]

      --disable-auto-includes

  -h, --help              Show this help message and exit.
  -i, --include[=<includedPaths>...]
                          Additional files or directories to include.
  -j, --jdk=<jdkString>
      --jar-opts=<jarFileOpts>

  -V, --version           Print version information and exit.
      --webapp-runner-version=<webappRunnerVersion>
                          The version of webapp-runner to use. Defaults to the
                            most recent version (9.0.80.0).
```


## Requirements

- Java 8u101 or higher (versions < u101 might experience difficulties displaying build log output)

## Configuration

### Heroku API Key
This plugin uses Heroku's Platform API and thus requires an API key to function. If you have the
[Heroku CLI](https://devcenter.heroku.com/articles/heroku-cli) installed and logged in with `heroku login`, the plugin will automatically
pick up your API key. Alternatively, you can use the `HEROKU_API_KEY` environment variable to set your API key:

```shell
$ HEROKU_API_KEY="xxx-xxx-xxxx" java -jar heroku-deploy-standalone-0.0.0.jar ...
```
