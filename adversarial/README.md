# Adversarial suite — can hydra survive a real attacker?

hydra bundles an **enforcement** device-integrity runtime: when it detects tampering
it **self-terminates the protected process**. This suite reproduces the four attack
classes a determined attacker actually uses against an on-device RASP, run against
the **hydra sample app** (`com.example.hydrasample`). For each, the expected result
is the same — the protected app **does not survive launch**.

| # | Attack | What it does | Expected result |
|---|---|---|---|
| 1 | **Repackage** (`repackage/`) | patch one `.text` instruction in the shipped native lib, re-zip, re-sign with the attacker's key — the no-root "ship a neutered APK" delivery | app detects the modified lib + foreign signer → **killed** |
| 2 | **Zygisk** (`zygisk-probe/`) | a native Zygisk module patches the protected lib's `.text` **in memory** at process start (the deepest on-device tamper) | live-code-hash mismatch → **killed** |
| 3 | **KernelSU module** (`ksu-module/`) | a KSU/Magisk module **bind-mounts** a patched native lib over the app's on-disk one | replaced lib fails self-integrity → **killed** |
| 4 | **LSPosed** (`lsposed-module/`) | an in-process ART method hook at Zygote fork — no frida artifacts | ART hook detected → **killed** |

## How to observe the result

After applying an attack and launching the app, the process should **not stay
alive**:

```bash
adb shell am start -n com.example.hydrasample/.MainActivity
sleep 4
adb shell pidof com.example.hydrasample && echo "SURVIVED (unexpected)" || echo "KILLED (expected)"
```

## Important: test on a device whose *only* compromise is the attack

The runtime kills on **any** integrity failure, so a device that is already rooted /
hooked will kill the genuine app too — you can't attribute the kill to the attack.
Run each vector on a **clean, unlocked test device** (or emulator) where the attack
you are applying is the *only* modification, then compare against the genuine app
running normally on the same device. Attacks 2–4 need root + the respective
framework (Zygisk / KernelSU / LSPosed) installed to load at all.

> Red-team / research use only. These modules tamper with a process on purpose;
> they are inert against any app other than the sample.
