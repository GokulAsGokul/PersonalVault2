# Personal Vault 2.0

A Kotlin Android application for securely managing personal and household passwords and documents.

> **Release baseline:** Production branch snapshot reviewed on 22 November 2026. This document describes source code present in the repository. It does not certify that Firebase resources have been deployed, that all workflows have passed end-to-end testing, or that the application has completed a formal security assessment.

## Current production baseline

| Item | Value |
|---|---|
| Repository | [GokulAsGokul/PersonalVault2](https://github.com/GokulAsGokul/PersonalVault2) |
| Branch | `production` |
| Baseline commit | `c69e9511bd535d1f6d15e25c4d2cca7245de760c` |
| Android application ID | `com.gokul.personalvault` |
| Version name / code | `0.1.0` / `1` |
| Minimum / target SDK | 26 / 36 |
| Compile SDK | 36 |
| JVM target | 17 |
| UI | Jetpack Compose, Material 3 |
| Backend | Firebase Authentication, Cloud Firestore, Firebase Storage, Cloud Functions |

## Current implemented functionality

### 1. Authentication and vault access
- Email/password registration and sign-in through Firebase Authentication.
- Email-verification checks before protected vault operations.
- Separate Firebase user identities.
- Personal V2 vault creation and active-vault resolution.
- Master-password setup and unlock.
- Encrypted local vault-key envelope and in-memory key session handling.
- Vault key session is cleared on sign-out.

### 2. Password Manager
- Create, view, edit and delete password records.
- Personal and Shared password scopes.
- AES-256-GCM encrypted password payloads.
- Personal records use the member vault key; Shared records use the household key.
- Search and category filtering.
- Sort controls.
- Password details with show/hide and copy actions.
- Custom categories.
- Password strength indicator.
- Optional expiry date and scheduled reminders at 7, 3 and 1 days before expiry (subject to notification permission and Android scheduling conditions).

### 3. Household membership and shared-device access
- One-time household join codes with 15-minute expiry and single-use handling.
- Join request submission from a separate authenticated account.
- Owner-side membership request listing, approval and rejection.
- Backend maximum of two household members.
- Device registration using device public keys.
- Owner approval of member devices.
- RSA-OAEP-256 wrapped household-key envelopes for approved devices.

**Important workflow distinction:** membership approval and device approval are separate. The owner must provision the household-key envelope after approving a member device.

### 4. Recovery
- Create and verify an encrypted recovery backup.
- Maintain an active recovery backup pointer.
- Restore the vault key with the recovery password.
- Import the restored key under a new local master password.

### 5. Documents Manager
- Upload one or multiple files using the Android document picker.
- Batch limit: 10 files and 200 MB; per-file limit: 50 MB.
- File summary with file names, sizes, remove controls, notes, Personal/Shared access, category and destination folder.
- Per-file upload progress and batch progress.
- Built-in and custom categories.
- Create, rename, move and delete empty folders; nested folders/subfolders.
- Move documents between folders.
- Search, sorting and file-type filtering.
- Recent files and category counts.
- Open/download and share documents using Android content intents.
- View document details, edit metadata and replace file content.
- Recycle Bin with restore and permanent deletion.
- Scheduled server-side purge of expired recycled documents (30-day lifecycle in the function implementation).
- Scan Document using Google ML Kit Document Scanner.
- Take Photo using Android camera capture and in-app preview.

### 6. App appearance and navigation
- Home, Passwords, Documents and Settings navigation.
- Material 3 UI with Light/Dark theme support.

## Firebase backend functions in source

The current Functions source defines callable operations for:
- Household join-code creation, redemption and cancellation.
- Membership request listing, approval and rejection.
- Active V2 vault resolution.
- Shared-device registration, listing and approval.
- Document category create, rename and delete.
- Document soft-delete, restore and permanent deletion.
- Empty-folder deletion.
- Scheduled expired-document purge.

Function source presence does not prove deployment. Confirm deployed functions and their region in the intended Firebase project before release.

## Not implemented in the current production source

The following broader product ideas are not included as completed features in this baseline:
- Finance Manager, bank accounts, income/expense tracking, transfers, India/Oman currency handling, lending/borrowing and credit-card management.
- Secure Notes module.
- Personal Information module.
- Excel/PDF data export.
- Password generator and password history.
- Completed app-level PIN/biometric lock flow.
- Completed device revocation and household-key rotation lifecycle.
- Verified offline-first synchronization and conflict resolution.

Do not present these items as available app features until implemented and tested.

## Build and verification

### Local Android build

```powershell
.\gradlew.bat clean
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

### Firebase Functions checks

```powershell
cd functions
npm ci
npm run lint
npm run build
```

The repository contains an Android CI workflow at `.github/workflows/android-ci.yml`. It is configured for manual dispatch (`workflow_dispatch`); no successful CI run is asserted by this README. The workflow includes Android unit/build checks, Functions lint/build, and emulator instrumentation execution in a separate job.

### Verification boundary at this baseline
- The upload flow on the feature branch was reported working by the project owner; the production branch points to that same commit.
- A separate clean build/test execution from the production branch and a complete production regression run remain release gates.
- Source files for Firestore rules, Storage rules, Functions and indexes are present. Their deployment state and live Firebase configuration were not independently verified.
- No formal penetration test, independent security audit or complete two-device acceptance report is claimed.

## Repository documentation
- [Production release status](docs/PRODUCTION-RELEASE.md)
- [File inventory and current feature matrix](PersonalVault2-Current-Inventory.xlsx) — generate/export this workbook from the release documentation package if it is not present in the repository.
- [Historical project status](docs/PROJECT-STATUS.md) — earlier review; superseded for current branch inventory by the production release document.
- [README-FIRST.md](README-FIRST.md) — historical Stage 1 starter guide, not the current app specification.

## Release discipline
- Treat `production` as the stable baseline.
- Develop new work on feature branches.
- Merge only after review, clean Android build/tests, Firebase checks and regression validation.
- Tag a release only after the production release gates are complete.
