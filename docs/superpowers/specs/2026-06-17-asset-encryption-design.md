# Hydra Asset Encryption — Design

**Status:** approved (2026-06-17)
**Repo:** hydra only (no DeviceIntelligenceRASP change) → ships as hydra 1.5.0
**Roadmap item:** "Asset encryption — encrypt bundled assets so they decrypt only after a clean sweep."

## Goal

Let a consumer mark specific bundled assets for encryption. The plaintext never
ships in the APK; the ciphertext decrypts at runtime **only after the first
detection sweep completes clean** (zero CRITICAL) — identical gating to
`Hydra.secret`. On a rooted / hooked / emulated / cloned / tampered device the
watchdog kills the process before decryption returns, so plaintext never
materializes.

## Public API

DSL (opt-in list):
```kotlin
hydra {
    encryptAssets { include("config.json", "models/model.tflite") }
}
```

Runtime accessor (added to the generated `Hydra` facade):
```java
// Blocks until the first clean sweep — call OFF the main thread (Dispatchers.IO).
byte[] data = Hydra.asset(context, "config.json");
```
`Context` is required to open the `AssetManager`; passing it keeps the whole
feature in generated/plugin code with **no DeviceIntelligence runtime change**.
`Hydra.secret` stays Context-free.

## Cipher — reuses the existing hash + whitebox key path

No stock AES; built from primitives already in the system.

- **Key:** `key32 = K.g(seedHex)` at runtime — the OLLVM-obfuscated native
  sweep-gated derivation (the same anchor `StrDec.g` uses; blocks until the clean
  sweep). Build side uses the byte-identical `DiBaker.gatedDexKey(seed)`.
- **Keystream (SHA-256 counter mode):**
  `KS = SHA256(key32 ‖ seed ‖ LE32(0)) ‖ SHA256(key32 ‖ seed ‖ LE32(1)) ‖ …`,
  truncated to the asset length. `cipher = plain ⊕ KS`. Each 32-byte block has an
  independent pseudorandom pad — unlike the repeating-key XOR used for strings,
  this is safe for large / known-plaintext files.
- **Integrity:** `tag = HMAC-SHA256(macKey, cipher)`,
  `macKey = SHA256(key32 ‖ "hydra-asset-mac-v1")`. Verified before returning;
  mismatch throws.

Build and runtime compute the identical keystream because both start from the
byte-identical 32-byte gated key and the same SHA-256 construction.

## Components

1. **`HydraExtension`** — add an `encryptAssets { include(...) }` DSL backed by a
   `SetProperty<String>` of asset relative paths.

2. **`EncryptAssetsTask`** (new) — a transform over the AGP merged-assets artifact
   (`SingleArtifact.ASSETS`). For each `include`d path found in the merged assets:
   - generate a fresh 32-byte seed; `key32 = DiBaker.gatedDexKey(seed)`;
   - SHA256-CTR encrypt the bytes; compute the HMAC tag;
   - write ciphertext to `assets/hydra/enc/<sha256(name)>.bin` in the output;
   - **omit the plaintext file** from the output (everything else copied through);
   - append `{name, encRelPath, seedHex, length, tagHex}` to a manifest file.
   A listed path that is not present fails the build (typo guard).

3. **`GenerateHydraSecretsTask`** (extended) — also reads the asset manifest and
   emits, into the same `Hydra` class:
   ```java
   public static byte[] asset(Context ctx, String name) {
       Entry e = A.get(name);                 // encPath, seedHex, len, tag
       if (e == null) throw new IllegalArgumentException(...);
       byte[] ct = readAsset(ctx, e.encPath); // AssetManager
       byte[] key = unhex(io.ssemaj.dx.K.g(e.seed)); // blocks until clean sweep
       verifyHmac(key, ct, e.tag);            // throws on mismatch
       return xorKeystream(key, e.seedBytes, ct); // SHA256-CTR
   }
   ```
   `K.g` is the only key source — gated by construction, no ungated path. The
   SHA-256 / HMAC / hex helpers are emitted as private static methods.

4. **`HydraPlugin`** — register `EncryptAssetsTask` per variant, wire it as the
   `SingleArtifact.ASSETS` transform, and feed its manifest output to the
   generator. Forward the `encryptAssets` set.

## Data flow

Build: merged assets → EncryptAssetsTask (encrypt listed, drop plaintext, emit
ciphertext + manifest) → packaged → DI bake instruments + re-signs (ciphertext
assets ride along unchanged). Generator reads manifest → `Hydra.asset`.

Runtime: `Hydra.asset(ctx, name)` → read ciphertext asset → `K.g(seed)` (blocks
until clean sweep) → HMAC verify → SHA256-CTR decrypt → bytes. Compromised device
→ killed before `K.g` returns.

## Error handling

- Unknown asset name → `IllegalArgumentException` (like `secret`).
- HMAC mismatch → `IllegalStateException` (ciphertext tampered).
- `include`d path absent at build → task fails with a clear message.
- All decryption is gated; there is no ungated asset path.

## Testing

- **Plugin unit (`EncryptAssetsTask`):** encrypt a sample byte[] then decrypt with
  the `DiBaker` key via the same SHA256-CTR → equals original; HMAC verifies;
  output dir omits the plaintext and contains the ciphertext + manifest.
- **Generator unit:** generated `Hydra` contains `asset(` and derives the key via
  `K.g(` (gated), never a raw/un-gated key.
- **Sample build:** add an encrypted asset to the sample; `assembleRelease` bakes
  clean; plaintext absent from the APK, ciphertext present.
- **On-device:** S24 → `Hydra.asset` returns the correct bytes; emulator → process
  killed before decryption (no plaintext).

## Out of scope (YAGNI)

Streaming `InputStream` API, wildcard/glob includes, per-asset key rotation,
encrypting `res/raw`. Can be added later.
