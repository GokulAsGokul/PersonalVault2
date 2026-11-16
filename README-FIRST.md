# Personal Vault 2.0 — Stage 1 starter

This is the first Android app foundation. It is a visual starter only:
- Kotlin + Jetpack Compose + Material 3
- Home, Passwords, Files, Finance and Settings navigation
- No Firebase connection yet
- No login, real storage, encryption, notifications or export yet
- Do not enter real passwords, documents or financial information

## Open it in Android Studio

1. Install the latest stable Android Studio.
2. Extract this ZIP to a normal folder, for example `Documents/PersonalVault2_Stage1`.
3. Open Android Studio.
4. Choose **Open** (not New Project).
5. Select the extracted `PersonalVault2_Stage1` folder — the folder containing `settings.gradle.kts`.
6. If Android Studio asks to trust the project, choose Trust.
7. Allow Gradle sync to finish. Gradle sync means Android Studio downloads the libraries needed by the project.
8. If asked to install Android SDK Platform 36, accept the installation.
9. Create an Android emulator using Device Manager, or connect an Android phone with USB debugging enabled.
10. Press the green **Run ▶** button and select the emulator or phone.

## What should appear

A screen titled **Personal Vault 2.0** with bottom navigation:
Home, Passwords, Files, Finance and Settings.

Tap the bottom items to switch between the preview screens.

## If Gradle sync fails

- Check internet connection.
- In Android Studio choose **File > Sync Project with Gradle Files**.
- Make sure Android Studio is using JDK 17 or the embedded JDK recommended by the installed Android Studio version.
- Copy the complete error text and share it with me. Do not randomly change dependency versions.

## Next stage

After this screen runs successfully, Stage 2 will connect a dedicated Firebase development project and implement separate email/password sign-in accounts. We will keep test data separate from real data.

## Important security note

This starter is not a password manager yet. It is not safe for real sensitive information. Production security requires properly designed encryption, key recovery, Firebase rules, backend permission checks, App Check and security testing.
