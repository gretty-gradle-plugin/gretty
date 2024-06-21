# gradle-java-toolchain

Simple gretty servlet application powered by gradle java toolchain.

## How to run

```bash
cd integrationTests/gradle-java-toolchain
gradle appRun
```


## How to test

```bash
cd integrationTests/gradle-java-toolchain
gradle integrationTest -PgeckoDriverPlatform=linux64 -PtoolchainJavaVersion=21
```
or
```bash
./docker_gradlew.sh --java 21 --java 11 --gradle 7 --working-dir integrationTests/gradle-java-toolchain -PtoolchainJavaVersion=21 -Pspock_version=2.3-groovy-3.0 -PgebVersion=5.1 integrationTest
```
## How to build a product


```bash
cd integrationTests/gradle-java-toolchain
gradle buildProduct
```

