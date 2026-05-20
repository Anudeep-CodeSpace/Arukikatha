# Releasing Arukikatha on GitHub

This project is distributed through GitHub Releases while Play Store publishing is deferred.

## Local Test Build

```bash
./gradlew test
./gradlew assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Public APK Guidance

For casual testing, a debug APK is installable because Android signs it with the local debug key.

For wider public distribution, create and keep a private release keystore outside Git, then produce a signed release APK. Never commit:

- `.jks`
- `.keystore`
- `keystore.properties`
- signing passwords

## Tag-Based GitHub Release

1. Ensure the app runs on a real Android device.
2. Update release notes.
3. Create a version tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The GitHub Actions workflow builds a debug APK artifact for tagged releases. Replace it with a signed release APK workflow once signing secrets are configured.

## Manual Release Checklist

- Run unit tests.
- Install the APK on a clean device.
- Verify timer start, pause, resume, stop, long-pause reset, tones, vibration, notification permission, and foreground notification.
- Add screenshots to the release.
- Mention that users may see Android warnings for apps installed outside Google Play.
