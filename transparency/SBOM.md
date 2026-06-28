# Software Bill of Materials — hydra native core (`libdicore.so`)

What is actually inside the obfuscated native core. It is **your protection logic
plus exactly two vendored third-party libraries** — nothing else is statically
linked in.

| Component | Version | Purpose | License |
|---|---|---|---|
| DeviceIntelligence runtime (`dicore`) | 4.2.0 | the protection logic itself (detection, integrity, watchdog) | proprietary (closed) |
| **mbed TLS** | 3.6.2 | X.509 parsing + signature verification for hardware **key-attestation** (no TLS/networking used) | Apache-2.0 (dual Apache-2.0 / GPL-2.0-or-later; used under Apache-2.0) |
| **miniz** | 3.0.2 | DEFLATE **inflate** only, for decompressed dex/`.so` hashing in App Bundle mode | MIT |

Notes:

- **mbed TLS is used for crypto/X.509 only — not for networking.** The vendored
  build compiles the X.509 + PK + hashing modules to verify the device's
  hardware key-attestation certificate chain on-device. No socket/TLS code path
  is reachable.
- **miniz is inflate-only.** It is compiled with `MINIZ_NO_DEFLATE_APIS`,
  `MINIZ_NO_ARCHIVE_APIS`, `MINIZ_NO_ZLIB_APIS`, `MINIZ_NO_STDIO`,
  `MINIZ_NO_TIME` — only `tinfl_*` (decompression) survives.
- There is **no analytics SDK, no networking library, no crash reporter, no
  advertising/identifier library** of any kind. Cross-check this against the
  complete imported-symbol list in
  [`libdicore-arm64-imports.txt`](libdicore-arm64-imports.txt).

Upstream license texts are vendored alongside the sources in the RASP project
(`third_party/mbedtls/LICENSE`, `third_party/miniz/LICENSE`).
