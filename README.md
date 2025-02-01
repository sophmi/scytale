# Scytale

Kotlin Multiplatform implementations of legacy cryptographic functions. All platforms are supported
(although not all are tested - see [Platform Support](#platform-support)).

⚠️ Scytale is **not** a secure, general-purpose cryptographic library: it's designed for
intergrating with legacy formats that utilise outdated cryptography (e.g. a file store that uses
Whirlpool as a checksum). See [Security](#security) for more information.

## Users

Artefacts coming soon™

### Platform Support
All platforms should be supported for downstream usage - simply consume the common artefact
regardless of your own targets.

Scytale is tested on:
- Kotlin/JVM
- Kotlin/JS (browser and Node.js)
- Kotlin/Wasm (browser and Node.js)

## Security

Scytale does not contain state-of-the-art cryptographic functions and should **not** be used in
place of audited libraries maintained by experts. Consequently, algorithms and/or implementations
may be vulnerable to side-channel attacks, significantly less secure than modern alternatives, or
cryptographically broken outright.

If your use case requires any level of security, this library is inappropriate.

## Contributing
Before proposing a new implementation, please open an issue to check that the algorithm will be
accepted - in particular, it must not be a modern one that users could reasonably assume would be
cryptographically-secure (e.g. ECDHE, AES-GCM).

See the [contribution guidelines](CONTRIBUTING.md) for further information.

## Licensing

Scytale is dual-licensed under [Apache 2.0](LICENSE-APACHE) and [MIT](LICENSE-MIT). Users may use
Scytale under the terms of either license, at their preference.
