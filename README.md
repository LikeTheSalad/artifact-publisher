Private lib used to publish other libs of "com.android.library" and "java-library" types.

Extension params:

```groovy
artifactPublisher {
    // Optionals (if not set, will be taken from the project's equivalent values)
    description = "some description"
    group = "come.group"
    version = "1.0.0"

    // Mandatory
    displayName = "some name"
    url = "http://some.url"
    vcsUrl = "http://some.vcs.url"
    issueTrackerUrl = "http://some.issue.url"
}
```

Command to publish:
```
./gradlew publishToMavenCentral
```

For publishing the artifact-publisher:
```
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
```