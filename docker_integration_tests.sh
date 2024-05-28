#!/usr/bin/env bash
set -e

export common_gradle_args="--console=plain --no-daemon -Porg.gradle.java.installations.auto-download=false -PgeckoDriverPlatform=linux64"

#ci.yml plugin build step
./docker_gradlew.sh                 \
  --java 11                         \
  --gradle 6                        \
  --gradle-home .docker-gradle      \
  $common_gradle_args               \
  build

#ci.yml matrix case #1
./docker_gradlew.sh                 \
  --java 8                          \
  --gradle 6                        \
  --gradle-home .docker-gradle      \
  --working-dir integrationTests    \
  $common_gradle_args               \
  -PtestAllContainers="\"['jetty9.3','jetty9.4','tomcat85','tomcat9']\"" \
  testAll
  
#ci.yml matrix case #2
./docker_gradlew.sh                 \
  --java 11                         \
  --gradle 6                        \
  --gradle-home .docker-gradle      \
  --working-dir integrationTests    \
  $common_gradle_args               \
  -PtestAllContainers="\"['jetty9.3','jetty9.4','jetty10','tomcat85','tomcat9']\"" \
  testAll

#ci.yml matrix case #3
./docker_gradlew.sh                 \
  --java 17                         \
  --gradle 7                        \
  --gradle-home .docker-gradle      \
  --working-dir integrationTests    \
  $common_gradle_args               \
  -PtestAllContainers="\"['jetty9.3','jetty9.4','jetty10','tomcat85','tomcat9']\"" \
  -Pspock_version=2.3-groovy-3.0 -PgebVersion=5.1 \
  testAll
   
#ci.yml matrix case #1 + toolchain java v21
./docker_gradlew.sh                 \
  --java 21 --java 8                \
  --gradle 6                        \
  --gradle-home .docker-gradle      \
  --working-dir integrationTests    \
  $common_gradle_args               \
  -PtestAllContainers="\"['jetty9.3','jetty9.4','tomcat85','tomcat9']\"" \
  -PtoolchainJavaVersion=21         \
  testAllJavaToolchain

#ci.yml matrix case #2 + toolchain java v17
./docker_gradlew.sh                 \
  --java 17 --java 11               \
  --gradle 6                        \
  --gradle-home .docker-gradle      \
  --working-dir integrationTests    \
  $common_gradle_args               \
  -PtestAllContainers="\"['jetty9.3','jetty9.4','jetty10','tomcat85','tomcat9']\"" \
  -PtoolchainJavaVersion=17         \
  testAllJavaToolchain
  
  
#ci.yml matrix case #3 + toolchain java v21
./docker_gradlew.sh                 \
  --java 21 --java 17               \
  --gradle 7                        \
  --gradle-home .docker-gradle      \
  --working-dir integrationTests    \
  $common_gradle_args               \
  -PtestAllContainers="\"['jetty9.3','jetty9.4','jetty10','tomcat85','tomcat9']\"" \
  -Pspock_version=2.3-groovy-3.0 -PgebVersion=5.1 \
  -PtoolchainJavaVersion=21       \
  testAllJavaToolchain