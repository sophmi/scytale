# Scytale

Kotlin Multiplatform implementations of uncommon or legacy cryptographic functions. All platforms
are supported (although not all are tested).

Security is explicitly a non-goal (see [Security](#security)); implementations prioritise 
correctness and intelligibility.

## Security

Scytale's intended use is for integration with existing applications and formats that utilise
legacy cryptographic functions and have few security needs (e.g. for interacting with a proprietary
file store that uses Whirlpool as a checksum).

Scytale does not contain modern state-of-the-art cryptographic functions and **should not** be used
in place of audited libraries maintained by experts. Consequently, security is not a primary goal
(use state-of-the-art cryptography instead) and side-channel attacks are of little concern.

Ensure your use case does not require greater security guarantees than this library provides.
