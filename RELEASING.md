## Setting up GPG

- The simplest way to sign is to use a GPG Agent with passphrase caching, and prime it before running a build with:

```
gpg -s
type something
CTRL-D
(enter passphrase)
```

## Running a release

- Set release version, drop `-SNAPSHOT`.
- Update all required doc files - see a previous release tag for an example of what should be updated.
- Final release commit should be signed with `-S -s`.
- Check build passes on GitHub Actions.
- Tag new release commit using `git tag -a -s -m "release ?.?.?" v?.?.?` or `git tag v?.?.? <commit-hash>` if using an existing commit.
- Checkout tag.
- Set JDK path to a JDK11 installation.

- Check `~/.gradle/gradle.properties` for credentials for `plugins.gradle.org` and `Sonatype` and for a GPG key name:

```
gradle.publish.key=<secret>
gradle.publish.secret=<secret>

ossrhUsername=javabrett
ossrhPassword=<secret>

signing.gnupg.useLegacyGpg=true
signing.gnupg.keyName=<secret>
```

- Build:

```
./gradlew build
```

- Release to Maven Central Staging:

```
./gradlew publishMavenJavaPublicationToMavenRepository
```

... then visit https://oss.sonatype.org/ to review uploads and approve/promote/release.

Wait until the new version is available at `https://repo1.maven.org/maven2/org/gretty/gretty/x.x.x/` (takes a while - maybe half an hour).

- Publish to `plugins.gradle.org`:

```
./gradlew publishPlugins
```

- Update a test-project to use the new Gretty version number and confirm download and build.
- Push tags: `git push origin --tags`.
- Update version on `master` to new version number with `-SNAPSHOT` suffix.
- Add/edit the release created on GitHub.
