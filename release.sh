#!/usr/bin/env bash

set -o pipefail
set -eu

mvn release:prepare release:perform

echo "Now make sure you update these articles and projects:

    https://github.com/kissaten/maven-plugin-war-example
    https://github.com/jkutner/travis-heroku-java-example
    https://devcenter.heroku.com/articles/deploying-java-applications-with-the-heroku-maven-plugin
    https://devcenter.heroku.com/articles/deploying-war-files-to-heroku-from-travis-ci
"
