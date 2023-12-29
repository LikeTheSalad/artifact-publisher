Internal lib used to publish libs of "com.android.library" and "java-library" types.

> NOTE: This is an internal tool configured for a single group id artifact.

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

Target extension params:

```groovy
artifactPublisherTarget {
    disablePublishing = false // defaults to false
}
```

Command to publish:

```
./gradlew publishToMavenCentral -Prelease=true
```

For publishing the artifact-publisher:

```
./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
```