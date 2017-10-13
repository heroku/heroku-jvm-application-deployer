#!/usr/bin/env bash

set -o pipefail
set -eu

mvn release:clean release:prepare release:perform

echo "Now make sure you update these articles and projects:

    https://github.com/kissaten/maven-plugin-example
    https://github.com/kissaten/maven-plugin-war-example
    https://devcenter.heroku.com/articles/deploying-java-applications-with-the-heroku-maven-plugin
"
