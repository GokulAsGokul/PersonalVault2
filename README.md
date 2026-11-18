# Personal Vault 2.0

An Android-based personal and household vault built with Kotlin, Jetpack Compose, Material 3 and Firebase.

> **Project status:** Active development. This README describes the implementation present in the reviewed source branch; it is not a guarantee that every feature has passed end-to-end testing or is deployed in Firebase.

## Current project baseline

- **Repository:** [GokulAsGokul/PersonalVault2](https://github.com/GokulAsGokul/PersonalVault2)
- **Branch reviewed:** `feature/approved-shared-device-access`
- **Baseline reviewed:** `db70c8c77de3725c4e7f5dafbdc77f45faa3a281`
- **Status report:** [docs/PROJECT-STATUS.md](docs/PROJECT-STATUS.md)

## Technology stack

- Kotlin, Android and Jetpack Compose
- Material 3
- Firebase Authentication
- Cloud Firestore
- Firebase Cloud Functions (callable functions in `asia-south1`)
- AES-256-GCM, Argon2id and RSA-OAEP-256 for the implemented key/encryption workflows

## Implemented features

### Authentication and vault foundation
- Email/password authentication UI and email-verification checks.
- Separate Firebase user identities.
- V2 personal vault creation and active-vault resolution.
- Master-password setup and unlock.
- Encrypted local vault-key envelope and in-memory key session.

### Password Manager
- Personal and Shared password scopes.
- Add and list encrypted password records.
- Personal records encrypted with the member's vault key.
- Shared records encrypted with the household key.
- Search and category filtering.
- Password details, show/hide and copy actions.

**Not yet present in the reviewed Password Manager:** edit, delete, password history and password generator.

### Household and shared-device access
- One-time household join codes with expiry.
- Membership request, owner approval and rejection.
- Maximum two household members enforced by backend logic.
- Per-device registration and owner approval.
- RSA-wrapped household-key envelopes and shared-key unlock workflow.

Membership approval and device approval are distinct. After a member device is approved, the owner must still provision the household-key envelope to that device.

### Recovery
- Encrypted recovery backup creation and verification.
- Active backup pointer.
- Recovery restore and local import under a new master password.

## Current UI/module status

| Module | Status in reviewed source |
|---|---|
| Home/dashboard | Implemented |
| Password Manager | Add, list, search, filter and view |
| Household | Membership and device workflows |
| Settings / Recovery | Backup and restore |
| Documents | Placeholder; management not implemented |
| Personal Information | Placeholder |
| Finance | No working V2 module identified |
| Secure Notes | No V2 implementation identified |

## Planned / pending work

1. Stabilize Password Manager permissions and add edit/delete.
2. Documents Manager: upload/download, folders, subfolders and preview.
3. Secure Notes: CRUD and attachments.
4. Finance: accounts, income, expenses, transfers, India/Oman currency handling and lending/borrowing.
5. Credit cards: statements, partial payments and due-date reminders.
6. Reports and Excel/PDF exports.
7. App PIN/biometric lock, menu-level permissions, device revocation and key rotation.
8. Offline synchronization and comprehensive security, integration and UI regression testing.

## Important Firebase verification note

The reviewed repository's `firestore.rules` includes vault, recovery, device and shared-key-envelope rules, but does not include a dedicated rule for the `vaultsV2/{vaultId}/passwords/{passwordId}` collection. The password repository uses the lowercase collection name `passwords`. Firestore paths are case-sensitive and unmatched paths are denied by default.

Before changing password functionality, compare the deployed Firestore rules and actual Firestore paths with the repository. Do not weaken approved-device checks or delete existing encrypted data as a workaround.

## Documentation

- [Current project status and detailed feature inventory](docs/PROJECT-STATUS.md)
- [Stage 1 starter guide](README-FIRST.md) — historical onboarding notes for the original visual starter; it does not describe the current V2 implementation.

## Review scope

This README and the linked status report are based on source code in the reviewed branch and commit. They do not certify deployed Cloud Functions, live Firestore rules/data, Storage configuration or end-to-end behaviour on both devices. Verify the Firebase environment and run regression tests before treating a feature as release-ready.
