# Scytale

Kotlin Multiplatform implementations of uncommon or legacy cryptographic functions. All platforms
are supported (although not all are tested - see [Platform Support](#platform-support)).

Security is explicitly a non-goal (see [Security](#security)); implementations prioritise 
correctness and intelligibility.

## Security

Scytale's intended use is for integration with existing applications and formats that utilise
legacy cryptographic functions and have few security needs (e.g. for interacting with a proprietary
file store that uses Whirlpool as a checksum).

Scytale does not contain modern state-of-the-art cryptographic functions and should **not** be used
in place of audited libraries maintained by experts. Consequently, security is not a primary goal; 
algorithms and implementations may be vulnerable to side-channel attacks, significantly less secure
than modern alternatives, or cryptographically broken outright.

Ensure your use case does not require greater security guarantees than this library provides.

## Users

Artefacts coming soon™

### Platform Support
All platforms should be supported for downstream usage - simply consume the common artefact
regardless of your own targets.

Scytale is tested on:
- Kotlin/JVM
- Kotlin/JS (browser and Node.js)
- Kotlin/Wasm (browser and Node.js)

## Licensing

Scytale is dual-licensed under [Apache 2.0](LICENSE-APACHE) and [MIT](LICENSE-MIT). Users may use
Scytale under the terms of either license, at their preference.
