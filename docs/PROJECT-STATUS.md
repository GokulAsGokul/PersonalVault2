# Personal Vault 2.0 — Current Project Status

**Review date:** 18 November 2026  
**Repository:** [GokulAsGokul/PersonalVault2](https://github.com/GokulAsGokul/PersonalVault2)  
**Reviewed branch:** `feature/approved-shared-device-access`  
**Reviewed baseline commit:** `db70c8c77de3725c4e7f5dafbdc77f45faa3a281`

> This document records what is present in the reviewed repository source. "Implemented" means implementation is present in source; it does not by itself certify that the feature has passed end-to-end testing or is currently working in a deployed environment. Firebase production/deployed configuration and live Firestore data were not independently inspected as part of this source review.

## 1. Technology and project structure

- Android application written in Kotlin.
- Jetpack Compose and Material 3 UI.
- Firebase Authentication for account sign-in.
- Cloud Firestore for V2 vault metadata and encrypted records.
- Firebase Cloud Functions (second-generation callable functions, `asia-south1`) for household membership and device workflows.
- Android Keystore-backed device key handling is used by the household crypto implementation.
- Encryption implementation includes AES-256-GCM, Argon2id and RSA-OAEP-256 key wrapping.

## 2. Implemented in the repository

### Authentication and vault foundation

- Email/password registration and sign-in UI.
- Email-verification enforcement in the application and callable backend.
- Separate Firebase user identities.
- V2 personal vault creation/loading.
- Server-side resolution of the active vault for an owner or approved member.
- Master-password setup and unlock.
- Local encrypted storage of the member's vault-key envelope.
- In-memory vault-key session handling and clearing on sign-out.
- AES-GCM encryption with Argon2id-derived wrapping key for the local master-password envelope.

Relevant source includes `MainActivity.kt`, `VaultRepositoryV2.kt`, `MasterPasswordV2Screen.kt`, `MasterPasswordCryptoV2.kt`, `VaultKeyLocalStoreV2.kt` and `VaultKeySessionV2.kt`.

### Password Manager

- Personal and Shared scopes/tabs.
- Add password records with website/app, username, password, category and notes.
- Encrypt records before writing them to Firestore.
- Use the member's vault key for personal records.
- Use the household key for shared records.
- Load personal records filtered by current owner UID and shared records by shared scope.
- Search by website/app, username and category.
- Category filter chips.
- Password details view, show/hide password, copy username and copy password.

Relevant source includes `PasswordManagerV2Screen.kt`, `PasswordRepositoryFirestoreV2.kt`, `PasswordRecordCryptoV2.kt`, `PasswordRecordV2.kt` and `EncryptedPasswordDocumentV2.kt`.

**Not present in the reviewed Password Manager UI/repository:** edit, delete, password history and password generator.

### Household membership

- Generate one-time join codes.
- Join-code expiry (15 minutes) and single-use handling.
- Submit a membership request from a separately authenticated account.
- Owner can list, approve or reject membership requests.
- Backend enforces a maximum of two household members.
- Membership changes are performed through callable Cloud Functions rather than direct client writes.
- Active vault lookup returns the owner's vault to an approved member.

Relevant source includes `HouseholdInvitationV2Screen.kt`, `HouseholdInvitationRepositoryV2.kt` and `functions/src/index.ts`.

### Shared-device approval and encrypted key exchange

- Register an Android device with a device-specific public key.
- Owner device is approved automatically; another member's device starts pending.
- Owner can list devices and approve a pending member device.
- Device public key is used to wrap the household key for that device.
- Shared-key envelope is stored separately for each member device.
- Client can attempt to unlock the household key from its device envelope.

Relevant source includes `SharedDeviceApprovalRepositoryV2.kt`, `HouseholdCrypto.kt` and the device-related callable functions in `functions/src/index.ts`.

**Workflow distinction:** approving household membership and approving an individual device are separate steps. Device approval alone does not create the encrypted key envelope; the owner must also provision the shared key to that approved device.

### Recovery

- Create an encrypted recovery envelope using a recovery password.
- Store and verify the encrypted backup.
- Maintain a pointer to the active recovery backup for the member.
- Restore the vault key using the recovery password.
- Import the recovered key under a new local master password.

Relevant source includes `RecoveryPasswordV2Screen.kt`, `RecoveryBackupRepositoryV2.kt` and `RecoveryCryptoV2.kt`.

## 3. UI areas and current implementation level

| Area | Current state in reviewed source |
|---|---|
| Home/dashboard | Implemented |
| Password Manager | Add, list, search, filter and view implemented |
| Household | Join-code and membership-request UI implemented |
| Device access | Device approval panel/workflow implemented |
| Settings / Recovery | Recovery backup and restore UI implemented |
| Documents | Placeholder screen; document management not implemented |
| Personal Information | Placeholder screen |
| Finance | No working V2 finance module identified in the reviewed dashboard/source |
| Secure Notes | No V2 implementation identified in the reviewed source |

## 4. Not yet implemented or not established by this source review

The following items are part of the broader product scope but were not found as completed V2 functionality in the reviewed source:

### Password Manager enhancements
- Edit and delete password records.
- Password history.
- Password generator.

### Documents
- File upload/download and storage lifecycle.
- Folder and subfolder management.
- Document preview and management UI.

### Secure Notes
- Create, edit and delete secure notes.
- Attachments.

### Finance and credit cards
- Multiple account/bank balance management.
- Income, expense and transaction management.
- Account-to-account transfers, including India–Oman transfers.
- INR/OMR currency conversion and manual rate override.
- Lending and borrowing tracking.
- Credit-card statements, partial payments and due-date reminders.
- Finance reports and dashboards.

### Platform capabilities
- Excel and PDF exports.
- Application PIN lock and completed biometric unlock flow.
- Menu-level permissions management.
- Verified offline-first operation and automatic sync/conflict handling.
- Completed notification/reminder delivery.
- Full device revocation and key-rotation lifecycle.
- Comprehensive end-to-end/UI regression test coverage.

The manifest declares biometric permission, but permission declaration alone is not a completed biometric lock implementation.

## 5. Firebase backend functions present in source

The reviewed `functions/src/index.ts` contains these V2 callable functions:

| Callable function | Responsibility |
|---|---|
| `createV2JoinCode` | Create a one-time household join code |
| `submitV2JoinCode` | Redeem a join code and submit membership request |
| `listMyV2MembershipRequests` | List owner-side pending requests |
| `approveV2Membership` | Approve membership and update membership mapping |
| `rejectV2Membership` | Reject a membership request |
| `cancelV2JoinCode` | Revoke active unused join codes |
| `getMyV2Vault` | Resolve the signed-in user's active V2 vault |
| `registerV2SharedDevice` | Register a device public key |
| `listV2SharedDevices` | List household member devices |
| `approveV2SharedDevice` | Approve a pending device |

This list describes functions found in source; deployment status should be checked separately in the Firebase project.

## 6. Security and configuration observations

1. The reviewed `firestore.rules` defines rules for vault root documents, member recovery data, device documents and shared-key envelopes.
2. The reviewed rules file does **not** define a `match /passwords/{passwordId}` rule. Unmatched paths are denied by default. Password access therefore depends on the deployed rules matching the repository and should be verified before relying on password writes/reads.
3. The password repository uses the lowercase Firestore subcollection name `passwords`. Firestore collection/document paths are case-sensitive; compare this with the actual deployed database path and deployed rules.
4. Shared-key envelope reads are restricted to vault members and approved device records. Do not weaken these checks merely to bypass a permission error.
5. The owner must provision an envelope after approving a member device; approval itself is not key provisioning.
6. Do not delete existing encrypted password records, device registrations, key envelopes or recovery backups as a troubleshooting shortcut. Diagnose the actual document path, identity and rule evaluation first.
7. The source review does not prove the deployed Firestore rules, deployed Cloud Functions, live document fields, Firebase Storage configuration or current device state. Those require separate environment checks.

## 7. Recommended next development sequence

1. **Stabilize Password Manager:** compare repository and deployed Firestore rules/paths; verify personal and shared access for both accounts; then add edit/delete with authorization and regression tests.
2. **Documents Manager:** upload/download, folders/subfolders, preview and access controls.
3. **Secure Notes:** CRUD and attachment handling.
4. **Finance Manager:** accounts, income/expenses, transfers, India/Oman currencies and lending/borrowing.
5. **Credit Card Manager:** statements, full/partial payments and reminders.
6. **Reports and exports:** monthly summaries, Excel and PDF.
7. **Application security:** PIN/biometric lock, permissions, device revocation and key rotation.
8. **Release readiness:** offline/sync validation, security-rule tests, integration tests, UI regression, performance and crash testing.

## 8. Review boundaries

This is a source-code inventory based on the reviewed branch and baseline commit stated above. It is not a live production audit, penetration test, full code-quality review or confirmation that every implemented feature works on both devices. The next step should be to reconcile the deployed Firebase configuration with this repository snapshot before changing code or security rules.
