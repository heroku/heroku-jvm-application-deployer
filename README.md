# Heroku JVM Application Deployer &emsp; ![License](https://img.shields.io/github/license/heroku/heroku-jvm-application-deployer) ![Maven Central](https://img.shields.io/maven-central/v/com.heroku/heroku-jvm-application-deployer) [![CI](https://github.com/heroku/heroku-jvm-application-deployer/actions/workflows/ci.yml/badge.svg)](https://github.com/heroku/heroku-jvm-application-deployer/actions/workflows/ci.yml)

Command line tool to deploy JVM applications directly to Heroku without pushing to a Git repository. It uses 
[Heroku's Platform API](https://devcenter.heroku.com/articles/platform-api-quickstart). This can be useful when deploying from a CI server, deploying pre-built JAR or WAR files.

It will automatically include and configure a Tomcat server via [webapp-runner](https://github.com/heroku/webapp-runner) if a WAR file is deployed.

> [!IMPORTANT]
> This repository previously contained Heroku's Maven Plugin for deploying JVM applications. This plugin is no longer
> maintained. Customers currently using the Maven Plugin can use the new Heroku JVM application deployer CLI to deploy
> JAR and WAR files to Heroku without using Git.
>
> The code of the last Heroku Maven plugin release can be found here:
> https://github.com/heroku/heroku-jvm-application-deployer/tree/3.0.7

## Table of Contents
* [Installation](#installation)
* [Usage](#usage)
* [Requirements](#requirements)
* [Configuration](#configuration)
  + [Heroku API Key](#heroku-api-key)

## Installation

Download the JAR file from the [latest release on GitHub](https://github.com/heroku/heroku-jvm-application-deployer/releases/latest). Older releases can be downloaded from the [GitHub releases](https://github.com/heroku/heroku-jvm-application-deployer/releases) list.

## Usage

```shell
java -jar heroku-jvm-application-deployer.jar --help
```

```
Usage: heroku-jvm-application-deployer [-dhV] [-a=name] [-j=string]
                                       [--jar-opts=options]
                                       [--webapp-runner-version=version] [-b
                                       [=buildpack...]]... [-i[=path...]]...
                                       [file]
Application for deploying Java applications to Heroku.
      [file]                The JAR or WAR file to deploy.
  -a, --app=name            The name of the Heroku app to deploy to. Defaults
                              to app name from git remote.
  -b, --buildpack[=buildpack...]
                            Defaults to heroku/jvm.
  -d, --disable-auto-includes
                            Disable automatic inclusion of certain files.
  -h, --help                Show this help message and exit.
  -i, --include[=path...]   Additional files or directories to include.
  -j, --jdk=string          Set the Heroku JDK selection string for the app (i.
                              e. 17, 21.0.1).
      --jar-opts=options    Add command line options for when the JAR is run.
  -V, --version             Print version information and exit.
      --webapp-runner-version=version
                            The version of webapp-runner to use. Defaults to
                              the most recent version (9.0.83.0).
```

## Requirements

- Java 8u101 or higher (versions < u101 might experience difficulties displaying build log output)

## Configuration

### Heroku API Key
This plugin uses Heroku's Platform API and thus requires an API key to function. If you have the
[Heroku CLI](https://devcenter.heroku.com/articles/heroku-cli) installed and logged in with `heroku login`, the plugin will automatically
pick up your API key. Alternatively, you can use the `HEROKU_API_KEY` environment variable to set your API key:

```shell
$ HEROKU_API_KEY="xxx-xxx-xxxx" java -jar heroku-jvm-application-deployer.jar ...
```
