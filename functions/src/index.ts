
/* eslint-disable max-len, require-jsdoc */

import {initializeApp} from "firebase-admin/app";
import {FieldValue, Timestamp, getFirestore} from "firebase-admin/firestore";
import {setGlobalOptions} from "firebase-functions";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {createHash, randomInt} from "node:crypto";

initializeApp();
const db = getFirestore();

setGlobalOptions({maxInstances: 5, region: "asia-south1"});

const JOIN_CODES = "householdJoinCodesV2";
const REQUESTS = "householdMembershipRequestsV2";
const MEMBERSHIPS = "vaultMembershipsV2";
const OWNER_VAULTS = "vaultsV2";

const MAX_MEMBERS = 2;
const JOIN_CODE_LIFETIME_MS = 15 * 60 * 1000;
const JOIN_CODE_LENGTH = 10;
const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

type CallableData = Record<string, unknown>;

function requireIdentity(request: {
  auth?: {uid: string; token: Record<string, unknown>} | null;
}) {
  const auth = request.auth;

  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Please sign in to continue.");
  }

  if (auth.token.email_verified !== true) {
    throw new HttpsError("failed-precondition", "Verify your email address first.");
  }

  return {uid: auth.uid};
}

function requireString(
  value: unknown,
  field: string,
  maxLength = 256
): string {
  if (typeof value !== "string") {
    throw new HttpsError("invalid-argument", `${field} is required.`);
  }

  const clean = value.trim();

  if (!clean || clean.length > maxLength) {
    throw new HttpsError("invalid-argument", `${field} is invalid.`);
  }

  return clean;
}

function hashCode(code: string): string {
  return createHash("sha256").update(code).digest("hex");
}

function generateJoinCode(): string {
  return Array.from(
    {length: JOIN_CODE_LENGTH},
    () => CODE_ALPHABET[randomInt(CODE_ALPHABET.length)]
  ).join("");
}

function timestampMillis(value: unknown): number | null {
  if (value instanceof Timestamp) {
    return value.toMillis();
  }
  return null;
}

function assertValidOwnerVault(
  vault: FirebaseFirestore.DocumentSnapshot,
  ownerUid: string
): string[] {
  if (!vault.exists || vault.get("ownerUid") !== ownerUid) {
    throw new HttpsError(
      "failed-precondition",
      "Your Personal Vault 2.0 owner vault is not ready. Open the app and try again."
    );
  }

  if (vault.get("schemaVersion") !== 2) {
    throw new HttpsError(
      "failed-precondition",
      "Your vault version is not supported."
    );
  }

  const members = vault.get("memberUids");

  if (!Array.isArray(members) || !members.includes(ownerUid)) {
    throw new HttpsError(
      "failed-precondition",
      "The owner vault membership is invalid."
    );
  }

  return members as string[];
}

/**
 * Owner generates a one-time join code.
 * The raw code is returned only once. Firestore stores its SHA-256 hash.
 */
export const createV2JoinCode = onCall(async (request) => {
  const {uid} = requireIdentity(request);

  const code = generateJoinCode();
  const codeHash = hashCode(code);

  const codeRef = db.collection(JOIN_CODES).doc(codeHash);
  const vaultRef = db.collection(OWNER_VAULTS).doc(uid);

  const activeCodesQuery = db.collection(JOIN_CODES)
    .where("ownerUid", "==", uid)
    .where("status", "==", "active");

  const expiresAt = Timestamp.fromMillis(
    Date.now() + JOIN_CODE_LIFETIME_MS
  );

  await db.runTransaction(async (tx) => {
    const vault = await tx.get(vaultRef);
    const activeCodes = await tx.get(activeCodesQuery);

    const members = assertValidOwnerVault(vault, uid);

    if (members.length >= MAX_MEMBERS) {
      throw new HttpsError(
        "resource-exhausted",
        "Your vault already has the maximum number of members."
      );
    }

    // A newly generated code invalidates previous unused codes.
    activeCodes.docs.forEach((doc) => {
      tx.update(doc.ref, {
        status: "revoked",
        revokedAt: FieldValue.serverTimestamp(),
      });
    });

    tx.create(codeRef, {
      ownerUid: uid,
      status: "active",
      createdAt: FieldValue.serverTimestamp(),
      expiresAt,
      redeemedByUid: null,
      redeemedAt: null,
    });
  });

  return {
    code,
    status: "active",
    expiresAt: expiresAt.toMillis(),
    expiresInSeconds: JOIN_CODE_LIFETIME_MS / 1000,
  };
});

/**
 * Wife submits the code while signed in with her own Firebase account.
 * The code is consumed immediately, before owner approval.
 */
export const submitV2JoinCode = onCall(async (request) => {
  const {uid} = requireIdentity(request);
  const data = (request.data ?? {}) as CallableData;

  const code = requireString(data.code, "Join code", 32)
    .replace(/[\s-]/g, "")
    .toUpperCase();

  if (
    code.length !== JOIN_CODE_LENGTH ||
    ![...code].every((character) => CODE_ALPHABET.includes(character))
  ) {
    throw new HttpsError("invalid-argument", "Enter a valid join code.");
  }

  const codeRef = db.collection(JOIN_CODES).doc(hashCode(code));
  const requestRef = db.collection(REQUESTS).doc();
  const memberRef = db.collection(MEMBERSHIPS).doc(uid);

  const result = await db.runTransaction(async (tx) => {
    const codeSnapshot = await tx.get(codeRef);

    if (!codeSnapshot.exists) {
      throw new HttpsError("not-found", "Join code not found.");
    }

    const codeData = codeSnapshot.data()!;
    const ownerUid = typeof codeData.ownerUid === "string" ?
      codeData.ownerUid : "";

    if (!ownerUid || ownerUid === uid) {
      throw new HttpsError(
        "invalid-argument",
        "You cannot join your own vault."
      );
    }

    if (codeData.status !== "active") {
      throw new HttpsError(
        "failed-precondition",
        "This join code has already been used or is no longer active."
      );
    }

    const expiry = codeData.expiresAt as Timestamp | undefined;

    if (!expiry || expiry.toMillis() <= Date.now()) {
      tx.update(codeRef, {
        status: "expired",
        expiredAt: FieldValue.serverTimestamp(),
      });
      return {status: "expired"};
    }

    const vaultRef = db.collection(OWNER_VAULTS).doc(ownerUid);

    const [vault, existingMembership] = await Promise.all([
      tx.get(vaultRef),
      tx.get(memberRef),
    ]);

    const members = assertValidOwnerVault(vault, ownerUid);

    if (members.length >= MAX_MEMBERS) {
      throw new HttpsError(
        "resource-exhausted",
        "This vault already has the maximum number of members."
      );
    }

    if (members.includes(uid)) {
      throw new HttpsError(
        "already-exists",
        "You are already a member of this vault."
      );
    }

    if (existingMembership.exists) {
      throw new HttpsError(
        "already-exists",
        "Your account is already connected to another vault."
      );
    }

    tx.update(codeRef, {
      status: "redeemed",
      redeemedByUid: uid,
      redeemedAt: FieldValue.serverTimestamp(),
    });

    tx.create(requestRef, {
      ownerUid,
      inviteeUid: uid,
      status: "awaiting_owner_approval",
      createdAt: FieldValue.serverTimestamp(),
      approvedAt: null,
      rejectedAt: null,
    });

    return {
      status: "awaiting_owner_approval",
      requestId: requestRef.id,
    };
  });

  if (result.status === "expired") {
    throw new HttpsError(
      "deadline-exceeded",
      "This join code has expired. Ask the owner for a new code."
    );
  }

  return result;
});

/**
 * Owner lists requests awaiting approval.
 */
export const listMyV2MembershipRequests = onCall(async (request) => {
  const {uid} = requireIdentity(request);

  const result = await db.collection(REQUESTS)
    .where("ownerUid", "==", uid)
    .where("status", "==", "awaiting_owner_approval")
    .orderBy("createdAt", "desc")
    .limit(25)
    .get();

  return {
    requests: result.docs.map((doc) => {
      const data = doc.data();

      return {
        requestId: doc.id,
        ownerUid: data.ownerUid ?? "",
        inviteeUid: data.inviteeUid ?? "",
        status: data.status ?? "",
        createdAt: timestampMillis(data.createdAt),
      };
    }),
  };
});

/**
 * Owner approves a membership request.
 * This updates the actual V2 vault and both membership records.
 */
export const approveV2Membership = onCall(async (request) => {
  const {uid} = requireIdentity(request);
  const data = (request.data ?? {}) as CallableData;

  const requestId = requireString(
    data.requestId,
    "Request ID",
    128
  );

  const requestRef = db.collection(REQUESTS).doc(requestId);
  const vaultRef = db.collection(OWNER_VAULTS).doc(uid);
  const ownerMembershipRef = db.collection(MEMBERSHIPS).doc(uid);

  const result = await db.runTransaction(async (tx) => {
    const membershipRequest = await tx.get(requestRef);

    if (!membershipRequest.exists) {
      throw new HttpsError("not-found", "Membership request not found.");
    }

    const requestData = membershipRequest.data()!;

    if (requestData.ownerUid !== uid) {
      throw new HttpsError(
        "permission-denied",
        "Only the vault owner can approve this request."
      );
    }

    if (requestData.status !== "awaiting_owner_approval") {
      throw new HttpsError(
        "failed-precondition",
        "This request is no longer awaiting approval."
      );
    }

    const inviteeUid = typeof requestData.inviteeUid === "string" ?
      requestData.inviteeUid : "";

    if (!inviteeUid || inviteeUid === uid) {
      throw new HttpsError(
        "failed-precondition",
        "The member account is invalid."
      );
    }

    const inviteeMembershipRef = db.collection(MEMBERSHIPS).doc(inviteeUid);

    const [vault, ownerMembership, inviteeMembership] = await Promise.all([
      tx.get(vaultRef),
      tx.get(ownerMembershipRef),
      tx.get(inviteeMembershipRef),
    ]);

    const members = assertValidOwnerVault(vault, uid);

    if (ownerMembership.exists &&
        (ownerMembership.get("ownerUid") !== uid ||
         ownerMembership.get("vaultId") !== uid ||
         ownerMembership.get("role") !== "owner")) {
      throw new HttpsError(
        "failed-precondition",
        "The owner membership record is inconsistent."
      );
    }

    if (inviteeMembership.exists &&
        (inviteeMembership.get("ownerUid") !== uid ||
         inviteeMembership.get("vaultId") !== uid ||
         inviteeMembership.get("role") !== "member")) {
      throw new HttpsError(
        "already-exists",
        "This account is already connected to another vault."
      );
    }

    if (!members.includes(inviteeUid) && members.length >= MAX_MEMBERS) {
      throw new HttpsError(
        "resource-exhausted",
        "Your vault already has the maximum number of members."
      );
    }

    const newMembers = members.includes(inviteeUid) ?
      members : [...members, inviteeUid];

    tx.update(vaultRef, {
      memberUids: newMembers,
      updatedAt: FieldValue.serverTimestamp(),
    });

    if (!ownerMembership.exists) {
      tx.create(ownerMembershipRef, {
        vaultId: uid,
        ownerUid: uid,
        role: "owner",
        createdAt: FieldValue.serverTimestamp(),
      });
    }

    if (!inviteeMembership.exists) {
      tx.create(inviteeMembershipRef, {
        vaultId: uid,
        ownerUid: uid,
        role: "member",
        createdAt: FieldValue.serverTimestamp(),
      });
    }

    tx.update(requestRef, {
      status: "approved",
      approvedAt: FieldValue.serverTimestamp(),
    });

    return {
      status: "approved",
      vaultId: uid,
      memberCount: newMembers.length,
    };
  });

  return result;
});

/**
 * Owner rejects a membership request.
 */
export const rejectV2Membership = onCall(async (request) => {
  const {uid} = requireIdentity(request);
  const data = (request.data ?? {}) as CallableData;

  const requestId = requireString(
    data.requestId,
    "Request ID",
    128
  );

  const requestRef = db.collection(REQUESTS).doc(requestId);

  await db.runTransaction(async (tx) => {
    const membershipRequest = await tx.get(requestRef);

    if (!membershipRequest.exists) {
      throw new HttpsError("not-found", "Membership request not found.");
    }

    if (membershipRequest.get("ownerUid") !== uid) {
      throw new HttpsError(
        "permission-denied",
        "Only the vault owner can reject this request."
      );
    }

    if (membershipRequest.get("status") !== "awaiting_owner_approval") {
      throw new HttpsError(
        "failed-precondition",
        "This request is no longer awaiting approval."
      );
    }

    tx.update(requestRef, {
      status: "rejected",
      rejectedAt: FieldValue.serverTimestamp(),
    });
  });

  return {status: "rejected"};
});

/**
 * Owner revokes any currently active unused join codes.
 */
export const cancelV2JoinCode = onCall(async (request) => {
  const {uid} = requireIdentity(request);

  const activeCodesQuery = db.collection(JOIN_CODES)
    .where("ownerUid", "==", uid)
    .where("status", "==", "active");

  const count = await db.runTransaction(async (tx) => {
    const activeCodes = await tx.get(activeCodesQuery);

    activeCodes.docs.forEach((doc) => {
      tx.update(doc.ref, {
        status: "revoked",
        revokedAt: FieldValue.serverTimestamp(),
      });
    });

    return activeCodes.size;
  });

  return {status: "revoked", count};
});

/**
 * Returns the active V2 vault for the signed-in account.
 * A member resolves to the owner's vault; an unconnected user resolves
 * to their own vault.
 */
export const getMyV2Vault = onCall(async (request) => {
  const {uid} = requireIdentity(request);

  const membershipRef = db.collection(MEMBERSHIPS).doc(uid);
  const membership = await membershipRef.get();

  let ownerUid = uid;
  let role = "owner";

  if (membership.exists) {
    const data = membership.data()!;

    if (data.role !== "owner" && data.role !== "member") {
      throw new HttpsError(
        "failed-precondition",
        "Your vault membership role is invalid."
      );
    }

    if (typeof data.ownerUid !== "string" ||
        data.vaultId !== data.ownerUid) {
      throw new HttpsError(
        "failed-precondition",
        "Your vault membership data is invalid."
      );
    }

    ownerUid = data.ownerUid;
    role = data.role;
  }

  const vault = await db.collection(OWNER_VAULTS).doc(ownerUid).get();
  const members = assertValidOwnerVault(vault, ownerUid);

  if (!members.includes(uid)) {
    throw new HttpsError(
      "permission-denied",
      "Your account is not a member of this vault."
    );
  }

  return {
    vaultId: ownerUid,
    ownerUid,
    memberUid: uid,
    role,
    memberUids: members,
    schemaVersion: 2,
  };
});


const DEVICE_ID_PATTERN = /^[A-Za-z0-9_-]{16,80}$/;
const BASE64_PATTERN = /^[A-Za-z0-9+/]+={0,2}$/;

/**
 * A member registers this installation's RSA public key. The private key
 * never leaves Android Keystore. The owner device is approved automatically;
 * every other member device starts pending owner approval.
 */
export const registerV2SharedDevice = onCall(async (request) => {
  const {uid} = requireIdentity(request);
  const data = (request.data ?? {}) as CallableData;
  const vaultId = requireString(data.vaultId, "Vault ID");
  const deviceId = requireString(data.deviceId, "Device ID", 80);
  const publicKey = requireString(data.publicKey, "Device public key", 8192);
  const algorithm = requireString(data.algorithm, "Key algorithm", 64);

  if (!DEVICE_ID_PATTERN.test(deviceId)) {
    throw new HttpsError("invalid-argument", "Device ID format is invalid.");
  }
  if (algorithm !== "RSA-OAEP-256") {
    throw new HttpsError("invalid-argument", "Unsupported device key algorithm.");
  }
  if (!BASE64_PATTERN.test(publicKey)) {
    throw new HttpsError("invalid-argument", "Device public key encoding is invalid.");
  }

  const publicKeyBytes = Buffer.from(publicKey, "base64");
  if (publicKeyBytes.length < 256 || publicKeyBytes.length > 4096) {
    throw new HttpsError("invalid-argument", "Device public key size is invalid.");
  }

  const vaultRef = db.collection(OWNER_VAULTS).doc(vaultId);
  const vault = await vaultRef.get();
  const members = assertValidOwnerVault(vault, vaultId);
  if (!members.includes(uid)) {
    throw new HttpsError("permission-denied", "You are not an approved vault member.");
  }

  const deviceRef = vaultRef.collection("members").doc(uid)
    .collection("devices").doc(deviceId);
  const existing = await deviceRef.get();
  if (existing.exists) {
    if (existing.get("publicKey") !== publicKey ||
        existing.get("algorithm") !== algorithm) {
      throw new HttpsError(
        "already-exists",
        "This device ID is already registered with a different key. Register a new device."
      );
    }
    if (existing.get("status") === "revoked") {
      throw new HttpsError(
        "permission-denied",
        "This device was revoked. Register a new device installation."
      );
    }
    return {status: existing.get("status") ?? "pending", deviceId};
  }

  const status = uid === vaultId ? "approved" : "pending";
  await deviceRef.create({
    vaultId,
    memberUid: uid,
    deviceId,
    publicKey,
    algorithm,
    status,
    createdAt: FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  });

  return {status, deviceId};
});

/** Owner lists registered devices and their approval states. */
export const listV2SharedDevices = onCall(async (request) => {
  const {uid} = requireIdentity(request);
  const vault = await db.collection(OWNER_VAULTS).doc(uid).get();
  const members = assertValidOwnerVault(vault, uid);
  const devices: Array<Record<string, unknown>> = [];

  for (const memberUid of members) {
    const snapshot = await db.collection(OWNER_VAULTS).doc(uid)
      .collection("members").doc(memberUid).collection("devices").get();
    snapshot.docs.forEach((doc) => {
      const data = doc.data();
      devices.push({
        memberUid,
        deviceId: doc.id,
        status: data.status ?? "unknown",
        algorithm: data.algorithm ?? "",
        publicKey: data.status === "approved" ? data.publicKey ?? "" : "",
        createdAt: timestampMillis(data.createdAt),
      });
    });
  }

  return {devices};
});

/** Owner approves one pending member installation, not the whole account. */
export const approveV2SharedDevice = onCall(async (request) => {
  const {uid} = requireIdentity(request);
  const data = (request.data ?? {}) as CallableData;
  const memberUid = requireString(data.memberUid, "Member UID");
  const deviceId = requireString(data.deviceId, "Device ID", 80);

  if (!DEVICE_ID_PATTERN.test(deviceId)) {
    throw new HttpsError("invalid-argument", "Device ID format is invalid.");
  }
  if (memberUid === uid) {
    throw new HttpsError("invalid-argument", "Owner devices are approved automatically.");
  }

  const vaultRef = db.collection(OWNER_VAULTS).doc(uid);
  const deviceRef = vaultRef.collection("members").doc(memberUid)
    .collection("devices").doc(deviceId);

  await db.runTransaction(async (tx) => {
    const [vault, device] = await Promise.all([
      tx.get(vaultRef),
      tx.get(deviceRef),
    ]);
    const members = assertValidOwnerVault(vault, uid);
    if (!members.includes(memberUid)) {
      throw new HttpsError("permission-denied", "This account is not a vault member.");
    }
    if (!device.exists || device.get("memberUid") !== memberUid ||
        device.get("vaultId") !== uid) {
      throw new HttpsError("not-found", "Registered device not found.");
    }
    if (device.get("status") !== "pending") {
      throw new HttpsError(
        "failed-precondition",
        "Only a pending device can be approved."
      );
    }
    tx.update(deviceRef, {
      status: "approved",
      approvedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
  });

  return {status: "approved", memberUid, deviceId};
});


