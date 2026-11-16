/* eslint-disable max-len, require-jsdoc */

import {initializeApp} from "firebase-admin/app";
import {FieldValue, Timestamp, getFirestore} from "firebase-admin/firestore";
import {setGlobalOptions} from "firebase-functions";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {createHash} from "node:crypto";

initializeApp();
const db = getFirestore();

setGlobalOptions({maxInstances: 5, region: "asia-south1"});

const INVITES = "householdInvitationsV2";
const HOUSEHOLDS = "householdsV2";
const MEMBERSHIPS = "householdMembershipV2";
const OWNER_VAULTS = "vaultsV2";
const INVITE_LOCKS = "householdInvitationLocksV2";
const MAX_MEMBERS = 2;
const INVITE_LIFETIME_MS = 7 * 24 * 60 * 60 * 1000;

type CallableData = Record<string, unknown>;

function requireIdentity(request: {auth?: {uid: string; token: Record<string, unknown>} | null}) {
  const auth = request.auth;
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Please sign in to continue.");
  }
  if (auth.token.email_verified !== true) {
    throw new HttpsError("failed-precondition", "Verify your email address first.");
  }
  const email = typeof auth.token.email === "string" ? auth.token.email.trim().toLowerCase() : "";
  if (!email) {
    throw new HttpsError("failed-precondition", "Your account must have a verified email address.");
  }
  return {uid: auth.uid, email};
}

function requireString(value: unknown, field: string, maxLength = 256): string {
  if (typeof value !== "string") {
    throw new HttpsError("invalid-argument", `${field} is required.`);
  }
  const clean = value.trim();
  if (!clean || clean.length > maxLength) {
    throw new HttpsError("invalid-argument", `${field} is invalid.`);
  }
  return clean;
}

function normalizeEmail(value: unknown): string {
  const email = requireString(value, "Invitee email", 254).toLowerCase();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    throw new HttpsError("invalid-argument", "Enter a valid email address.");
  }
  return email;
}

function inviteSnapshotData(snapshot: FirebaseFirestore.DocumentSnapshot) {
  const data = snapshot.data() ?? {};
  return {
    invitationId: snapshot.id,
    ownerUid: data.ownerUid ?? "",
    inviteeEmail: data.inviteeEmail ?? "",
    inviteeUid: data.inviteeUid ?? null,
    status: data.status ?? "",
    createdAt: data.createdAt?.toMillis?.() ?? null,
    expiresAt: data.expiresAt?.toMillis?.() ?? null,
  };
}

async function assertOwnerVault(ownerUid: string) {
  const vault = await db.collection(OWNER_VAULTS).doc(ownerUid).get();
  if (!vault.exists || vault.get("ownerUid") !== ownerUid) {
    throw new HttpsError(
      "failed-precondition",
      "Your Personal Vault 2.0 owner vault is not ready. Open the app and try again."
    );
  }
  const members = vault.get("memberUids");
  if (!Array.isArray(members) || !members.includes(ownerUid)) {
    throw new HttpsError("failed-precondition", "The owner vault membership is invalid.");
  }
}

export const createV2EmailInvitation = onCall(async (request) => {
  const {uid, email: ownerEmail} = requireIdentity(request);
  const data = (request.data ?? {}) as CallableData;
  const inviteeEmail = normalizeEmail(data.inviteeEmail);

  if (inviteeEmail === ownerEmail) {
    throw new HttpsError("invalid-argument", "You cannot invite your own email address.");
  }

  await assertOwnerVault(uid);

  const membershipRef = db.collection(MEMBERSHIPS).doc(uid);
  const householdRef = db.collection(HOUSEHOLDS).doc(uid);
  // A deterministic lock prevents two concurrent requests creating duplicate active invitations.
  const lockId = createHash("sha256")
    .update(`${uid}:${inviteeEmail}`)
    .digest("hex");
  const lockRef = db.collection(INVITE_LOCKS).doc(lockId);
  const inviteRef = db.collection(INVITES).doc();
  const expiresAt = Timestamp.fromMillis(Date.now() + INVITE_LIFETIME_MS);

  await db.runTransaction(async (tx) => {
    const membership = await tx.get(membershipRef);
    const household = await tx.get(householdRef);
    const lock = await tx.get(lockRef);

    if (membership.exists && (membership.get("ownerUid") !== uid ||
        membership.get("householdId") !== uid || membership.get("role") !== "owner")) {
      throw new HttpsError("already-exists", "You are already connected to another household.");
    }

    const members = household.exists ? household.get("memberUids") : [uid];
    if (!Array.isArray(members) || !members.includes(uid)) {
      throw new HttpsError("failed-precondition", "Household membership data is invalid.");
    }
    if (members.length >= MAX_MEMBERS) {
      throw new HttpsError("resource-exhausted", "Your household already has the maximum number of members.");
    }

    if (lock.exists && typeof lock.get("invitationId") === "string") {
      const existingRef = db.collection(INVITES).doc(lock.get("invitationId"));
      const existing = await tx.get(existingRef);
      if (existing.exists) {
        const existingStatus = existing.get("status");
        const existingExpiry = existing.get("expiresAt") as Timestamp | undefined;
        const stillActive = ["pending", "awaiting_owner_approval"].includes(existingStatus) &&
          !!existingExpiry && existingExpiry.toMillis() > Date.now();
        if (stillActive) {
          throw new HttpsError("already-exists", "There is already an active invitation for this email address.");
        }
      }
    }

    tx.create(inviteRef, {
      ownerUid: uid,
      ownerEmail,
      inviteeEmail,
      inviteeUid: null,
      status: "pending",
      createdAt: FieldValue.serverTimestamp(),
      expiresAt,
      respondedAt: null,
      approvedAt: null,
    });
    tx.set(lockRef, {
      ownerUid: uid,
      inviteeEmail,
      invitationId: inviteRef.id,
      updatedAt: FieldValue.serverTimestamp(),
    });
  });

  return {invitationId: inviteRef.id, expiresAt: expiresAt.toMillis(), status: "pending"};
});

export const listMyV2Invitations = onCall(async (request) => {
  const {email} = requireIdentity(request);
  const result = await db.collection(INVITES)
    .where("inviteeEmail", "==", email)
    .orderBy("createdAt", "desc")
    .limit(25)
    .get();

  const now = Date.now();
  return {
    invitations: result.docs.map((doc) => {
      const item = inviteSnapshotData(doc);
      if (item.status === "pending" && typeof item.expiresAt === "number" && item.expiresAt <= now) {
        return {...item, status: "expired"};
      }
      return item;
    }),
  };
});

export const respondToV2Invitation = onCall(async (request) => {
  const {uid, email} = requireIdentity(request);
  const data = (request.data ?? {}) as CallableData;
  const invitationId = requireString(data.invitationId, "Invitation ID", 128);
  const response = requireString(data.response, "Response", 16).toLowerCase();

  if (response !== "accept" && response !== "decline") {
    throw new HttpsError("invalid-argument", "Response must be accept or decline.");
  }

  const inviteRef = db.collection(INVITES).doc(invitationId);
  const result = await db.runTransaction(async (tx) => {
    const invite = await tx.get(inviteRef);
    if (!invite.exists) {
      throw new HttpsError("not-found", "Invitation not found.");
    }

    const inviteData = invite.data()!;
    if (inviteData.inviteeEmail !== email) {
      throw new HttpsError("permission-denied", "This invitation belongs to a different email address.");
    }
    if (inviteData.status !== "pending") {
      throw new HttpsError("failed-precondition", "This invitation is no longer pending.");
    }

    const expiry = inviteData.expiresAt as Timestamp | undefined;
    if (!expiry || expiry.toMillis() <= Date.now()) {
      tx.update(inviteRef, {status: "expired", respondedAt: FieldValue.serverTimestamp()});
      return {status: "expired"};
    }

    tx.update(inviteRef, {
      status: response === "accept" ? "awaiting_owner_approval" : "declined",
      inviteeUid: uid,
      respondedAt: FieldValue.serverTimestamp(),
    });
    return {status: response === "accept" ? "awaiting_owner_approval" : "declined"};
  });

  return result;
});

export const listMyV2MembershipRequests = onCall(async (request) => {
  const {uid} = requireIdentity(request);
  const result = await db.collection(INVITES)
    .where("ownerUid", "==", uid)
    .where("status", "==", "awaiting_owner_approval")
    .orderBy("respondedAt", "desc")
    .limit(25)
    .get();

  return {requests: result.docs.map(inviteSnapshotData)};
});

export const approveV2Membership = onCall(async (request) => {
  const {uid} = requireIdentity(request);
  const data = (request.data ?? {}) as CallableData;
  const invitationId = requireString(data.invitationId, "Invitation ID", 128);

  await assertOwnerVault(uid);

  const inviteRef = db.collection(INVITES).doc(invitationId);
  const householdRef = db.collection(HOUSEHOLDS).doc(uid);
  const ownerMembershipRef = db.collection(MEMBERSHIPS).doc(uid);

  const result = await db.runTransaction(async (tx) => {
    const invite = await tx.get(inviteRef);
    const household = await tx.get(householdRef);
    const ownerMembership = await tx.get(ownerMembershipRef);

    if (!invite.exists) {
      throw new HttpsError("not-found", "Invitation not found.");
    }
    const item = invite.data()!;
    if (item.ownerUid !== uid) {
      throw new HttpsError("permission-denied", "Only the invitation owner can approve it.");
    }
    if (item.status !== "awaiting_owner_approval") {
      throw new HttpsError("failed-precondition", "This invitation is not awaiting approval.");
    }

    const expiry = item.expiresAt as Timestamp | undefined;
    if (!expiry || expiry.toMillis() <= Date.now()) {
      tx.update(inviteRef, {status: "expired"});
      return {status: "expired"};
    }

    const inviteeUid = typeof item.inviteeUid === "string" ? item.inviteeUid : "";
    if (!inviteeUid || inviteeUid === uid) {
      throw new HttpsError("failed-precondition", "The invitee account is invalid.");
    }

    const inviteeMembershipRef = db.collection(MEMBERSHIPS).doc(inviteeUid);
    const inviteeMembership = await tx.get(inviteeMembershipRef);
    if (inviteeMembership.exists && (inviteeMembership.get("ownerUid") !== uid ||
        inviteeMembership.get("householdId") !== uid || inviteeMembership.get("role") !== "member")) {
      throw new HttpsError("already-exists", "This account is already connected to another household.");
    }
    if (ownerMembership.exists && (ownerMembership.get("ownerUid") !== uid ||
        ownerMembership.get("householdId") !== uid || ownerMembership.get("role") !== "owner")) {
      throw new HttpsError("failed-precondition", "Your household owner membership is inconsistent.");
    }

    const currentMembers = household.exists ? household.get("memberUids") : [uid];
    if (!Array.isArray(currentMembers) || !currentMembers.includes(uid)) {
      throw new HttpsError("failed-precondition", "Household membership data is invalid.");
    }
    if (currentMembers.includes(inviteeUid)) {
      // Repair missing membership references if the household member was already recorded.
      if (!ownerMembership.exists) {
        tx.create(ownerMembershipRef, {
          householdId: uid, ownerUid: uid, role: "owner", createdAt: FieldValue.serverTimestamp(),
        });
      }
      if (!inviteeMembership.exists) {
        tx.create(inviteeMembershipRef, {
          householdId: uid, ownerUid: uid, role: "member", createdAt: FieldValue.serverTimestamp(),
        });
      }
      tx.update(inviteRef, {status: "approved", approvedAt: FieldValue.serverTimestamp()});
      return {status: "approved", householdId: uid, memberCount: currentMembers.length};
    }
    if (currentMembers.length >= MAX_MEMBERS) {
      throw new HttpsError("resource-exhausted", "Your household already has the maximum number of members.");
    }

    const newMembers = [...currentMembers, inviteeUid];
    const householdData = {
      ownerUid: uid,
      memberUids: newMembers,
      schemaVersion: 2,
      createdAt: household.exists ? household.get("createdAt") : FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    };

    if (household.exists) {
      tx.set(householdRef, householdData, {merge: true});
    } else {
      tx.create(householdRef, householdData);
    }

    if (!ownerMembership.exists) {
      tx.create(ownerMembershipRef, {
        householdId: uid,
        ownerUid: uid,
        role: "owner",
        createdAt: FieldValue.serverTimestamp(),
      });
    }
    if (!inviteeMembership.exists) {
      tx.create(inviteeMembershipRef, {
        householdId: uid,
        ownerUid: uid,
        role: "member",
        createdAt: FieldValue.serverTimestamp(),
      });
    }

    tx.update(inviteRef, {status: "approved", approvedAt: FieldValue.serverTimestamp()});
    return {status: "approved", householdId: uid, memberCount: newMembers.length};
  });

  return result;
});

export const rejectV2Membership = onCall(async (request) => {
  const {uid} = requireIdentity(request);
  const data = (request.data ?? {}) as CallableData;
  const invitationId = requireString(data.invitationId, "Invitation ID", 128);
  const inviteRef = db.collection(INVITES).doc(invitationId);

  await db.runTransaction(async (tx) => {
    const invite = await tx.get(inviteRef);
    if (!invite.exists) {
      throw new HttpsError("not-found", "Invitation not found.");
    }
    if (invite.get("ownerUid") !== uid) {
      throw new HttpsError("permission-denied", "Only the invitation owner can reject it.");
    }
    if (invite.get("status") !== "awaiting_owner_approval") {
      throw new HttpsError("failed-precondition", "This request is not awaiting approval.");
    }
    tx.update(inviteRef, {status: "rejected", rejectedAt: FieldValue.serverTimestamp()});
  });

  return {status: "rejected"};
});

export const cancelV2Invitation = onCall(async (request) => {
  const {uid} = requireIdentity(request);
  const data = (request.data ?? {}) as CallableData;
  const invitationId = requireString(data.invitationId, "Invitation ID", 128);
  const inviteRef = db.collection(INVITES).doc(invitationId);

  await db.runTransaction(async (tx) => {
    const invite = await tx.get(inviteRef);
    if (!invite.exists) {
      throw new HttpsError("not-found", "Invitation not found.");
    }
    if (invite.get("ownerUid") !== uid) {
      throw new HttpsError("permission-denied", "Only the invitation owner can cancel it.");
    }
    const status = invite.get("status");
    if (status !== "pending" && status !== "awaiting_owner_approval") {
      throw new HttpsError("failed-precondition", "This invitation can no longer be cancelled.");
    }
    tx.update(inviteRef, {status: "cancelled", cancelledAt: FieldValue.serverTimestamp()});
  });

  return {status: "cancelled"};
});
