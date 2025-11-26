#!/usr/bin/env bash
set -e

export common_gradle_args="--console=plain --no-daemon -Porg.gradle.java.installations.auto-download=false -PgeckoDriverPlatform=linux64"

#ci.yml plugin build step
./docker_gradlew.sh                 \
  --java 17                         \
  --gradle 7                        \
  --gradle-home .docker-gradle      \
  $common_gradle_args               \
  publishToPrivateRepo

#ci.yml matrix case #2
./docker_gradlew.sh                 \
  --java 17                         \
  --gradle 7                        \
  --gradle-home .docker-gradle      \
  --working-dir integrationTests    \
  $common_gradle_args               \
  testAll

#ci.yml matrix case #3
./docker_gradlew.sh                 \
  --java 17                         \
  --gradle 8                        \
  --gradle-home .docker-gradle      \
  --working-dir integrationTests    \
  $common_gradle_args               \
  testAll

# a set of tests with java toolchain:

#ci.yml matrix case #2 + toolchain java v21
./docker_gradlew.sh                 \
  --java 21 --java 17               \
  --gradle 7                        \
  --gradle-home .docker-gradle      \
  --working-dir integrationTests    \
  $common_gradle_args               \
  -PtoolchainJavaVersion=21         \
  testAllJavaToolchain

#ci.yml matrix case #3 + toolchain java v21
./docker_gradlew.sh                 \
  --java 21 --java 17               \
  --gradle 8                        \
  --gradle-home .docker-gradle      \
  --working-dir integrationTests    \
  $common_gradle_args               \
  -PtoolchainJavaVersion=21         \
  testAllJavaToolchain
