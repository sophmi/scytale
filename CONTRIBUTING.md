# Contributors guide

Please note that unlike some Gradle-based projects, a Gradle wrapper is not included in this
repository. If you don't already have a local installation, one can be downloaded from
<https://gradle.org/install>.

## Feature proposals

Please [open an issue](https://github.com/sophmi/scytale/issues) before proposing a notable new
feature (e.g. support for a new cryptographic function), and ensure the feature suits the project
goals of supporting uncommon or legacy implementations. In particular, proposals must not concern
state-of-the-art cryptographic algorithms that users could reasonably assume would use a secure
implementation (e.g. ECDHE, AES-GCM).

Note that proposed algorithms do not have to be broken to be accepted, just niche/outdated.

## Development

### Requirements
- [Gradle](https://gradle.org/install)
- [Headless Chromium](https://kotlinlang.org/docs/js-project-setup.html#test-task) (or a standard
  Chrome installation), for JS and Wasm browser tests

### Building

Scytale is tested on:

- Kotlin/JVM
- Kotlin/JS (browser and Node.js)
- Kotlin/Wasm (browser and Node.js)

During development: ```gradle check```.

Before committing: ```gradle build```

#### Dependency updates

The [gradle versions plugin](https://github.com/ben-manes/gradle-versions-plugin) can be used to
automatically identify stale dependencies, using `gradle dependencyUpdates` (or `gradle dU`).

### Licensing

By making a contribution, you agree to dual-license that contribution under [Apache 2.0](LICENSE-APACHE)
and [MIT](LICENSE-MIT) (copyright is still retained by you).
