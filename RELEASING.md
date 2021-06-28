## Running a release

- Set release version, drop `-SNAPSHOT`.
- Update all required doc files - see a previous release tag for an example of what should be updated.
- Final release commit should be signed with `-S -s`.
- Check build passes on GitHub Actions.
- Tag release using `git tag -a -s -m "release ?.?.?" v?.?.?`.
- Checkout tag.
- Set JDK path to a JDK8 installation.
- `./gradlew build`
- Export required variables.  I precede these with a space and have `HISTCONTROL=ignorespace` so they won't appear in my history:

```
 export BINTRAY_USER="javabrett"
 export BINTRAY_KEY="<secret>"
 export BINTRAY_REPO="maven"
 export BINTRAY_PACKAGE="org.gretty"
 export GPG_PASSPHRASE="<secret>"
```

- Check `~/.gradle/gradle.properties` for credentials for plugins.gradle.org:

```
gradle.publish.key=<secret>
gradle.publish.secret=<secret>
```

- Push to bintray (again I lead with a space):

```
 ./gradlew bintrayUpload -PbintrayUser=${BINTRAY_USER} -PbintrayKey=${BINTRAY_KEY} -PbintrayRepo=${BINTRAY_REPO} -PbintrayPackage=${BINTRAY_PACKAGE} -PgpgPassphrase="${GPG_PASSPHRASE}"
```

- Publish to plugins.gradle.org:

```
 ./gradlew publishPlugins
```

- Release files on Bintray - login and release stages files.
- Update a test-project to use the new Gretty version number and confirm download and build.
- Push tags: `git push origin --tags`.
- Update version on `master` to new version number with `-SNAPSHOT` suffix.
- Update version links in [README.md](README.md).
- Add/edit the release created on GitHub.

## Transition documentation - uploading to Maven Central

Bintray and JCenter are dead for releases, so upload to Maven Central is being implemented.

In order to deploy you need:

- Minimum of these properties in ~/.gradle/gradle.properties:

```
ossrhUsername=javabrett
ossrhPassword=<secret>

signing.gnupg.useLegacyGpg=true
signing.gnupg.keyName=<secret>
```

The simplest way to sign is to use a GPG Agent with passphrase caching, and prime it before running a build with:

```
gpg -s
type something
CTRL-D
(enter passphrase)
```

To release to Maven Central Staging, after build run:

```
./gradlew uploadArchives
```

... then visit https://oss.sonatype.org/ to review uploads and approve/promote/release.
