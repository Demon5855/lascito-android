/**
 * Reclamación de una herencia por parte del guardián.
 *
 * La verificación del PIN y el conteo de intentos ocurren aquí, nunca en el
 * cliente: la app no lee `accessPinHash` ni escribe `status` / `failedAttempts`.
 *
 * - El hash bcrypt vive en `users/{creatorUid}/private/inheritance`, ilegible
 *   para cualquier cliente (ver firestore.rules).
 * - El estado y los intentos viven en `legacyInvitations/{invitationId}`,
 *   que el guardián puede leer pero no escribir.
 */

import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import bcrypt from "bcryptjs";

const MAX_PIN_ATTEMPTS = 3;
const PIN_REGEX = /^\d{4}$/;

type ClaimFailureReason =
    | "NOT_FOUND"
    | "ALREADY_CLAIMED"
    | "BLOCKED"
    | "EXPIRED"
    | "NOT_CONFIGURED"
    | "INVALID_PIN";

interface ClaimedInheritance {
    invitationId: string;
    creatorUid: string;
    creatorName: string;
    vaultContentRef: string;
    claimedAt: number;
}

type ClaimOutcome =
    | { ok: true; inheritance: ClaimedInheritance }
    | { ok: false; reason: ClaimFailureReason; attemptsRemaining?: number };

export const claimInheritance = onCall(
    { maxInstances: 10 },
    async (request) => {
        const auth = request.auth;
        if (!auth) {
            throw new HttpsError("unauthenticated", "Authentication required.");
        }

        const email = auth.token.email?.toLowerCase();
        if (!email || auth.token.email_verified !== true) {
            throw new HttpsError("failed-precondition", "Verified email required.", {
                reason: "EMAIL_NOT_VERIFIED"
            });
        }

        const invitationId = request.data?.invitationId;
        const pin = request.data?.pin;
        if (
            typeof invitationId !== "string" || invitationId.length === 0 || invitationId.includes("/") ||
            typeof pin !== "string" || !PIN_REGEX.test(pin)
        ) {
            throw new HttpsError("invalid-argument", "invitationId and a 4-digit pin are required.");
        }

        const db = admin.firestore();
        const invitationRef = db.collection("legacyInvitations").doc(invitationId);

        // El resultado se devuelve desde la transacción (en vez de lanzar dentro)
        // para que el incremento de intentos fallidos quede confirmado.
        const outcome = await db.runTransaction<ClaimOutcome>(async (transaction) => {
            const invitationSnap = await transaction.get(invitationRef);
            const invitation = invitationSnap.data();

            // Una invitación ajena se trata igual que una inexistente.
            if (!invitation || invitation.guardianEmail !== email) {
                return { ok: false, reason: "NOT_FOUND" };
            }

            const failedAttempts: number = invitation.failedAttempts ?? 0;
            if (invitation.status === "BLOCKED" || failedAttempts >= MAX_PIN_ATTEMPTS) {
                return { ok: false, reason: "BLOCKED", attemptsRemaining: 0 };
            }
            if (invitation.status === "CLAIMED") {
                return { ok: false, reason: "ALREADY_CLAIMED" };
            }
            if (invitation.status !== "PENDING") {
                return { ok: false, reason: "EXPIRED" };
            }

            const now = Date.now();
            if (typeof invitation.expiresAt === "number" && now > invitation.expiresAt) {
                transaction.update(invitationRef, { status: "EXPIRED" });
                return { ok: false, reason: "EXPIRED" };
            }

            const secretSnap = await transaction.get(
                db.doc(`users/${invitation.creatorUid}/private/inheritance`)
            );
            const accessPinHash = secretSnap.get("accessPinHash");
            if (typeof accessPinHash !== "string" || accessPinHash.length === 0) {
                // Sin PIN configurado no hay nada que adivinar: no cuenta como intento.
                return { ok: false, reason: "NOT_CONFIGURED" };
            }

            const isPinValid = await bcrypt.compare(pin, accessPinHash);

            if (!isPinValid) {
                const newFailedAttempts = failedAttempts + 1;
                const isBlocked = newFailedAttempts >= MAX_PIN_ATTEMPTS;
                transaction.update(invitationRef, {
                    failedAttempts: newFailedAttempts,
                    lastFailedAttempt: now,
                    ...(isBlocked ? { status: "BLOCKED", blockedAt: now } : {})
                });
                return isBlocked ?
                    { ok: false, reason: "BLOCKED", attemptsRemaining: 0 } :
                    {
                        ok: false,
                        reason: "INVALID_PIN",
                        attemptsRemaining: MAX_PIN_ATTEMPTS - newFailedAttempts
                    };
            }

            transaction.update(invitationRef, {
                status: "CLAIMED",
                claimedAt: now,
                claimedByUid: auth.uid,
                failedAttempts: 0,
                lastFailedAttempt: admin.firestore.FieldValue.delete()
            });

            return {
                ok: true,
                inheritance: {
                    invitationId,
                    creatorUid: invitation.creatorUid ?? "",
                    creatorName: invitation.creatorName ?? "",
                    vaultContentRef: invitation.vaultContentRef ?? "",
                    claimedAt: now
                }
            };
        });

        if (outcome.ok) {
            logger.info("[claimInheritance] Herencia reclamada", { invitationId, uid: auth.uid });
            return outcome.inheritance;
        }

        logger.warn("[claimInheritance] Reclamación rechazada", {
            invitationId,
            uid: auth.uid,
            reason: outcome.reason
        });

        const details = {
            reason: outcome.reason,
            attemptsRemaining: outcome.attemptsRemaining ?? null
        };

        switch (outcome.reason) {
        case "NOT_FOUND":
            throw new HttpsError("not-found", "No pending inheritance found.", details);
        case "INVALID_PIN":
            throw new HttpsError("permission-denied", "Invalid PIN.", details);
        case "BLOCKED":
            throw new HttpsError("resource-exhausted", "Too many failed attempts.", details);
        default:
            throw new HttpsError("failed-precondition", "Inheritance cannot be claimed.", details);
        }
    }
);
