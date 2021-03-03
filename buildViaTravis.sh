#!/bin/bash
# This script will build the project.

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  echo -e "Build Pull Request #$TRAVIS_PULL_REQUEST => Branch ['$TRAVIS_BRANCH']"
  ./gradlew build
else
  # Only publish when using JDK8
  if [[ "$TRAVIS_JDK_VERSION" == *"jdk8" ]]; then
    if [ "$TRAVIS_TAG" == "" ]; then
      echo -e 'Build Branch with Snapshot => Branch ['$TRAVIS_BRANCH']'
      # snapshot
      ./gradlew -Prelease.travisci=true -PnetflixOss.username=$NETFLIX_OSS_REPO_USERNAME -PnetflixOss.password=$NETFLIX_OSS_REPO_PASSWORD  -Psonatype.signingPassword=$NETFLIX_OSS_SIGNING_PASSWORD -Prelease.scope=patch build snapshot 
    else
      echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] Tag ['$TRAVIS_TAG']'
      if [[ "$TRAVIS_TAG" == *"rc."* ]]; then
        # candidate
        ./gradlew -Prelease.travisci=true -Prelease.useLastTag=true  -PnetflixOss.username=$NETFLIX_OSS_REPO_USERNAME -PnetflixOss.password=$NETFLIX_OSS_REPO_PASSWORD  -Psonatype.signingPassword=$NETFLIX_OSS_SIGNING_PASSWORD candidate
      else
        # final
        ./gradlew -Prelease.travisci=true -Prelease.useLastTag=true -PnetflixOss.username=$NETFLIX_OSS_REPO_USERNAME -PnetflixOss.password=$NETFLIX_OSS_REPO_PASSWORD  -Psonatype.username=$NETFLIX_OSS_SONATYPE_USERNAME -Psonatype.password=$NETFLIX_OSS_SONATYPE_PASSWORD -Psonatype.signingPassword=$NETFLIX_OSS_SIGNING_PASSWORD final
      fi
    fi
  else
    echo -e "Build Branch without publishing => Branch ['$TRAVIS_BRANCH'] JDK ['$TRAVIS_JDK_VERSION']"
    ./gradlew build
  fi
fi
