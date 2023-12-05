# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

* Specifying a main JAR or WAR file is no longer required. This allows easy deployment in cases where application startup is managed by a shell script, such as when using [`sbt-native-packager`](https://github.com/sbt/sbt-native-packager). ([#258](https://github.com/heroku/heroku-jvm-application-deployer/pull/258))

## [4.0.0] - 2023-11-27

This project used to contain both a Maven Plugin and a CLI tool for deploying Java applications to Heroku without using Git.

Starting with this version, it no longer contains the Maven Plugin. Users are expected to migrate to the improved command line tool (formerly known as `heroku-deploy-standalone`). Previously, the Maven plugin and CLI supported different features and were maintained at different levels. This release unifies everything into a new CLI. This allows customers to use the same tool for a variety of JVM languages, independent of their build tool of choice.

See the [README](README.md) for a brief overview or refer to the `--help` output of the CLI for usage info.

Historic changelog entries are preserved below for completeness. But be aware that this release substantially changed the
nature of this project.

### Changed

* Use correct `Heroku-Deploy-Type` header when deploying. ([#247](https://github.com/heroku/heroku-jvm-application-deployer/pull/247))
* Update release process to add the JAR file to GitHub releases. ([#240](https://github.com/heroku/heroku-jvm-application-deployer/pull/240))
* Add proper CLI to configure all aspects of app deployment. Previously used Java properties are no longer supported. See `--help` for usage. ([#232](https://github.com/heroku/heroku-jvm-application-deployer/pull/232))
* Unify usage between WAR and JAR files. heroku-jvm-application-deployer will now automatically use the correct mode based on file extension. ([#232](https://github.com/heroku/heroku-jvm-application-deployer/pull/232))
* Default `webapp-runner` version is now always the most recently released version. ([#232](https://github.com/heroku/heroku-jvm-application-deployer/pull/232))

## [3.0.7] - 2023-02-06

### Changed 

* Update dependencies.

## [3.0.6] - 2022-12-01

### Changed

* Update dependencies.

## [3.0.5] - 2022-10-26

### Changed

* Update dependencies. ([#142](https://github.com/heroku/heroku-jvm-application-deployer/pull/142))

## [3.0.4] - 2020-08-11

### Fixed

* Fix `heroku:deploy-war` goal. ([#77](https://github.com/heroku/heroku-jvm-application-deployer/pull/77))

## [3.0.3] - 2020-07-09

### Fixed

* Add TLS workaround for OpenJDK 11.0.2. ([#72](https://github.com/heroku/heroku-jvm-application-deployer/pull/72))

## [3.0.2] - 2020-03-30

### Fixed

* Fix Microsoft Windows support. ([#62](https://github.com/heroku/heroku-jvm-application-deployer/pull/62))

## [3.0.1] - 2020-03-11

### Fixed

* Fix missing javadoc. ([#59](https://github.com/heroku/heroku-jvm-application-deployer/pull/59))

## [3.0.0] - 2020-03-11

### Changed

* Extended documentation
* Extensive refactoring, but mostly compatible with 2.x. See below for breaking changes.

### Removed

* HTTP_PROXY and HTTPS_PROXY environment variables are no longer respected. Use [standard JVM proxy
properties](https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html) to configure HTTP proxies.
* System property `heroku.curl.enabled` removed.
* Dropped support for Heroku Toolbelt on Windows. Use Heroku CLI instead.
* Support for `jvm-common` buildpack alias removed. Use standard `heroku/jvm` instead.

## [2.0.16] - 2020-01-07

### Changed

* Upgrade to Tomcat Webapp Runner 9.0.30.0

## [2.0.6] - 2018-08-27

### Changed

* Upgrade to Tomcat Webapp Runner 8.5.33.0
* Update default buildpack URL

## [0.5.0] - 2015-07-14

### Changed

* Upgraded to Tomcat 8 as default container

## [0.4.4] - 2015-07-01

### Added

* Added a warning for deploy-war when custom processTypes are present

## [0.4.3] - 2015-06-02

### Added

* Added ability to define custom buildpacks or multi-buildpack

### Fixed

* Accounted for Procfile's with empty lines in parsing

## [0.4.1] - 2015-05-12

### Added

* Added app name detection from Git repo.

## [0.4.0] - 2015-05-11

### Added

* Added ability to log incremental progress of slug upload.

## [0.3.6] - 2015-04-03

### Changed

* Change config vars to overwrite existing values by default
* Ignore POM packaging if `warFile` config is set

## [0.3.5] - 2015-03-05

### Changed

* Replaced javax.net with Apache HttpClient
* Improved default JAVA_TOOL_OPTIONS

## [0.3.4] - 2015-02-10

### Added

* Added `<includeTarget>` option to toggle the inclusion of the `target/` directory by default

### Changed

* Upgraded webapp-runner to 7.0.57.2

## [0.3.3] - 2015-02-09

### Added

*  Added commit string to slug meta-data

## [0.3.2] - 2015-01-29

### Added

*  Included with_jmap and with_jstack scripts.
*  Added `heroku:create-slug` and `heroku:deploy-slug` goals

## [0.3.1] - 2015-01-15

### Added

* Added `heroku:run-war` goal to run a WAR file locally with a command similar on Heroku
* Added `heroku:dashboard` goal to open the Heroku dashboard for the configured application
* Added `heroku:eclipse-launch-config` goal to generate Eclipse launch configuration files
* Added caching of the JDK so it won't be downloaded on every deploy 

### Changed

* Began enforcement of `war` packaging when using `-war` goals 
* Switched from custom MojoExecutor to org.twdata.maven:mojo-executor

## [0.3.0] - 2014-12-18

### Changed

*  Jumping to 0.3.x version to align with sbt-heroku
*  Use explicit versions in pom.xml

### Removed

*  Remove support for Java 1.6

## [0.1.9] - 2014-12-02

### Changed

*  Improved detection of Heroku API key. Now uses .netrc first.

[unreleased]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v4.0.0...HEAD
[4.0.0]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v3.0.7...v4.0.0
[3.0.7]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v3.0.6...v3.0.7
[3.0.6]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v3.0.5...v3.0.6
[3.0.5]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v3.0.4...v3.0.5
[3.0.4]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v3.0.3...v3.0.4
[3.0.3]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v3.0.2...v3.0.3
[3.0.2]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v3.0.1...v3.0.2
[3.0.1]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v3.0.0...v3.0.1
[3.0.0]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v2.0.16...v3.0.0
[2.0.16]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v2.0.6...v2.0.16
[2.0.6]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.6.0...v2.0.6
[0.5.0]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.4.4...v0.5.0
[0.4.4]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.4.3...v0.4.4
[0.4.3]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.4.2...v0.4.3
[0.4.2]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.4.1...v0.4.2
[0.4.1]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.4.0...v0.4.1
[0.4.0]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.3.6...v0.4.0
[0.3.6]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.3.5...v0.3.6
[0.3.5]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.3.3...v0.3.5
[0.3.4]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.3.2...v0.3.4
[0.3.3]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.1.9...v0.3.0
[0.1.9]: https://github.com/heroku/heroku-jvm-application-deployer/compare/v0.1.8...v0.1.9
