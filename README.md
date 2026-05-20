# Arukikatha

Arukikatha is a focused Android interval timer for the Japanese walking routine of alternating brisk and normal walking.

The app guides a session with:

- 3-minute brisk walking rounds
- 3-minute normal walking rounds
- 5-second transition pauses
- audio and vibration cues
- a 10-second long-pause reset rule
- 30 successful minutes tracked from completed rounds, not wall-clock time

## Install

For now, Arukikatha is distributed through GitHub Releases.

1. Open the latest release on this repository.
2. Download the attached APK.
3. On your Android phone, allow installing apps from your browser or file manager.
4. Open the APK and install it.

Android may show a warning because this app is installed outside Google Play. Only install APKs from releases you trust.

## Screenshots

Screenshots will be added after the first release build is captured from a real device.

Recommended screenshots:

- landing screen
- ready timer screen
- active brisk walking mode
- active normal walking mode
- pause/reset state

## Development

Requirements:

- Android Studio or JDK 17+
- Android SDK with API 35

Common commands:

```bash
./gradlew test
./gradlew assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release builds should be signed with a keystore that is never committed to Git.

## Release Process

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Run tests locally.
3. Create a Git tag such as `v1.0.0`.
4. Push the tag to GitHub.
5. Download the APK artifact from GitHub Actions or attach a signed APK to the GitHub Release.

## Privacy

Arukikatha is designed to work locally on-device. See [docs/privacy.md](docs/privacy.md).

## License

MIT License. See [LICENSE](LICENSE).
