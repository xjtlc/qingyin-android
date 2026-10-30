# Android 0.8.0 baseline inputs

This project uses a locally supplied `base.apk` stored outside the public repository (0.8.0-rc1, version code 80) as its baseline. A preserved copy is stored in `baseline/Qingyin-0.8.0-rc1-original.apk`. The original user file is not modified.

- `app/src/main/assets/node/runner.cjs` is copied byte for byte from that APK. It retains the 0.8.0 recommendation, artist, album, and music comment APIs; it is not replaced by the older 0.7.37 runtime or desktop code.
- `app/src/main/jniLibs/arm64-v8a/` contains the same APK's original `libnode.so`, `libqingyin_node.so`, and `libc++_shared.so`. No native library is rebuilt or changed.
- Manifest components and resources are decoded from the same APK with JADX 1.5.6. The two comment activities and their `adjustResize` setting are retained, as are notification/media/foreground-service permissions, all existing activities, the DUMP-protected debug receiver, and the original network configuration and resource IDs.
- Generated version and SDK attributes are supplied by Gradle. The source manifest retains application/component behavior; debuggable status is controlled by the build type.

`NON_JAVA_RECOVERY_REPORT.json` records the original file's SHA-256, the preserved copy's SHA-256, and the exact hashes of each runtime/native input. Changes ported from 0.7.37 are Java/UI changes and do not replace these runtime inputs.

The new build is 0.8.0-rc8, code 87, package `local.qingyin.mobile`, ARM64, minimum SDK 26, target SDK 34. The local build configuration uses Android Gradle Plugin 8.5.2, Gradle 8.7, JDK 17, and SDK 34. `build-debug.ps1` compiles against the existing Gradle cache without rebundling the backend. Android debug signing uses the existing default `%USERPROFILE%\.android\debug.keystore`; no private key has been copied into this project.

The original APK does not contain the author's Gradle or C++ source project, so the build scripts are reconstructed. Integration has successfully compiled, verified the original signing certificate, installed the update, and checked it on an Android 14 emulator. The rc8 changes address status-bar blending, glass/queue rendering costs, VSYNC-driven record animation, and lifecycle-scoped refresh-rate requests. Native and backend inputs remain unchanged. See README.md for the scope of runtime checks.
