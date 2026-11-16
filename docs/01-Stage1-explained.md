# Stage 1 explained in simple language

## What we have prepared

- A basic Android app project.
- A home screen and five navigation sections.
- A modern Android interface built with Jetpack Compose and Material 3.
- A structure that we can extend in later stages.

## What is not working yet

The Passwords, Files, Finance and Settings screens are only previews. They do not save data. Login and cloud sync are not connected. Nothing is encrypted because no sensitive data should be entered at this stage.

## Why build in stages?

If we connect every feature at once, it becomes difficult to find errors. We will first confirm that the app opens, then add login, then secure access, then each feature.

## Our next milestone

1. Create a Firebase project for development.
2. Register the Android app in Firebase using application ID `com.gokul.personalvault`.
3. Download `google-services.json` into the `app/` folder.
4. Enable Email/Password sign-in.
5. Connect Firebase Authentication.
6. Test two separate user accounts.

Never share `google-services.json` publicly if it contains project configuration; do not share passwords, service-account keys or private encryption keys. The Firebase client configuration file is not a substitute for security rules.
