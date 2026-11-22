# Personal Vault 2.0 — Production Release Status

**Review date:** 22 November 2026  
**Repository:** [GokulAsGokul/PersonalVault2](https://github.com/GokulAsGokul/PersonalVault2)  
**Branch:** `production`  
**Source baseline:** `c69e9511bd535d1f6d15e25c4d2cca7245de760c`

> This is a source-baseline and release-candidate status report. It is not a production deployment certification, security audit, or proof that every feature has passed end-to-end testing.

## 1. Release identification

| Item | Value |
|---|---|
| Android application ID | `com.gokul.personalvault` |
| Version name / code | `0.1.0 / 1` |
| Minimum SDK | 26 |
| Target / compile SDK | 36 / 36 |
| JVM target | 17 |
| Stack | Kotlin, Jetpack Compose, Material 3, Firebase Authentication, Firestore, Storage and Cloud Functions |

## 2. Current implemented functionality

### Authentication and vault foundation
- Email/password registration and sign-in.
- Email-verification checks before protected vault operations.
- V2 personal vault creation/loading and active-vault resolution.
- Master-password setup/unlock.
- Encrypted local vault-key envelope and in-memory key session.

### Password Manager
- Create, view, edit and delete password records.
- Personal and Shared scopes.
- AES-256-GCM encrypted password payloads; personal records use the member vault key and shared records use the household key.
- Search, category filtering, sorting and custom categories.
- Password details, show/hide and copy actions.
- Password strength indicator.
- Optional expiry date and WorkManager reminders at 7, 3 and 1 days before expiry.

### Household and shared-device access
- One-time join codes with 15-minute expiry and single-use handling.
- Join request submission from a separate authenticated account.
- Owner-side membership request listing, approval and rejection.
- Backend maximum of two household members.
- Device registration and owner approval.
- RSA-OAEP-256 wrapped household-key envelopes for approved devices.

**Important:** membership approval and device approval are separate. The owner must provision the household-key envelope after approving a member device.

### Recovery
- Create and verify encrypted recovery backups.
- Active recovery backup pointer.
- Restore vault key with recovery password and import under a new local master password.

### Documents Manager
- Android document picker for single/multiple files.
- Limits: 10 files per batch, 50 MB per file and 200 MB per batch.
- File summary with file names/sizes, remove controls, Personal/Shared access, category, destination folder and notes.
- Per-file and overall batch upload progress.
- Built-in/custom categories; create, rename and delete custom categories.
- Create, rename and move folders; nested folders/subfolders; delete empty folders; move documents.
- Search, sorting, file-type filtering, recent files and category counts.
- Open/download/share; document details, metadata edit and file replacement.
- Recycle Bin with restore and permanent deletion.
- Scheduled expired-document purge logic in Functions source.
- Google ML Kit Document Scanner and camera photo capture with in-app preview.

### Navigation and appearance
- Home, Passwords, Documents and Settings navigation.
- Material 3 Light/Dark theme definitions.

## 3. Not implemented as completed functionality

Do not list these as currently available features:
- Finance Manager, bank accounts, income/expenses, transfers, India/Oman currency handling, lending/borrowing or credit cards.
- Secure Notes.
- Personal Information module.
- Excel/PDF exports.
- Password generator and password history.
- Completed app-level PIN/biometric lock workflow.
- Verified offline-first synchronization and conflict resolution.
- Complete device revocation and household-key rotation lifecycle.

## 4. Build and test evidence

- The project owner reported the current document upload summary/progress flow working on the feature branch; `production` was created from the same source commit.
- A clean build and test run from the production branch was not independently executed as part of this documentation review.
- Unit test source exists for master-password, password-record and recovery cryptography.
- A login instrumentation smoke test exists.
- `.github/workflows/android-ci.yml` is configured for manual `workflow_dispatch`; no successful workflow run for the baseline commit was confirmed.
- Firebase rules, indexes, Storage rules and Functions source exist in the repository; deployment to the intended production Firebase project was not verified.
- Branch protection was not enabled at the time of review.
- No independent penetration test or formal security audit is evidenced.

## 5. Required release gates

1. On `production`, run `.\\gradlew.bat clean`, `.\\gradlew.bat test` and `.\\gradlew.bat assembleDebug`; retain successful logs.
2. From `functions/`, run `npm ci`, `npm run lint` and `npm run build`.
3. Run `connectedDebugAndroidTest` on an emulator and manual regression on both household accounts.
4. Verify Firebase project alias and deployed Firestore rules, Storage rules, indexes and Cloud Functions; confirm scheduled purge function and region.
5. Test personal/shared authorization boundaries, membership/device approval and recovery.
6. Test upload limits, multi-file failures, progress accuracy, folder placement, scanner, camera, download/share, recycle-bin restore and permanent deletion.
7. Confirm backup/restore procedures, privacy/data-retention expectations and Firebase monitoring.
8. Enable production branch protection and require reviewed pull requests plus passing CI checks.
9. Tag a release only after the applicable gates pass.

## 6. Operational notes

- Source presence does not prove Firebase deployment; verify deployed Functions, rules, indexes and Storage configuration.
- Membership approval does not automatically provision a shared-key envelope.
- Notification delivery depends on Android notification permission and OS scheduling.
- Google ML Kit scanner UI is provider/system UI; app-owned preview/upload UI is theme-controlled.

## 7. Release recommendation

**HOLD formal v1.0.0 production release until the required gates above are completed.**

The `production` branch is the controlled source baseline for validation. Once the production build, automated checks, Firebase deployment verification, two-account regression and branch protection are complete, approve a tagged release.
