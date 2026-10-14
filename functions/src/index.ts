/**
 * ═══════════════════════════════════════════════════════════════════════════
 * LASCITO - DEAD MAN'S SWITCH CLOUD FUNCTION (Production Grade v2.2)
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * MANEJO DEL PIN Y SU PISTA:
 * - El hash del PIN vive en `users/{uid}/private/inheritance` y solo lo lee
 *   la función `claimInheritance`; nunca se copia a la invitación
 * - pinHint está ofuscado con AES-256-CBC en Firestore. La clave se deriva de
 *   SHA-256(creatorUid), por lo que el servidor puede derivarla: NO es cifrado
 *   de extremo a extremo
 * - La Cloud Function desencripta la pista para enviarla por email
 * 
 * @version 2.2.0
 * @author Lascito Engineering Team
 * @lastModified 2026-03-09
 */

import { onSchedule } from "firebase-functions/v2/scheduler";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import { randomUUID } from "crypto";
import * as crypto from "crypto";  // ✅ NUEVO: Importar módulo crypto
import { sendGuardianEmail, RESEND_API_KEY, EMAIL_PROVIDER } from "./guardianEmail";

// ═══════════════════════════════════════════════════════════════════════════
// INICIALIZACIÓN DE FIREBASE ADMIN SDK
// ═══════════════════════════════════════════════════════════════════════════
if (admin.apps.length === 0) {
    admin.initializeApp();
}

const db = admin.firestore();

// ═══════════════════════════════════════════════════════════════════════════
// CONSTANTES DE CONFIGURACIÓN
// ═══════════════════════════════════════════════════════════════════════════
const PAGE_SIZE = 500;
const BATCH_SIZE = 500;
const MAX_EMAIL_RETRIES = 3;
const FUNCTION_TIMEOUT = 540;
const FUNCTION_MEMORY = "1GiB";

// ═══════════════════════════════════════════════════════════════════════════
// INTERFACES TYPESCRIPT
// ═══════════════════════════════════════════════════════════════════════════
interface DeadManSwitch {
    active: boolean;
    inactivityThresholdDays: number;
    lastProofOfLife: number;
    notificationSentAt?: number;
    processingStartedAt?: number;
    lastEmailError?: string;
    lastEmailErrorAt?: number;
}

interface LegacyGuardian {
    email: string;
    name: string;
    encryptedPinHint?: string | null;  // ✅ NUEVO: Pista encriptada (Base64)
    pinHintIv?: string | null;          // ✅ NUEVO: Vector de inicialización (Base64)
    status: string;
}

interface UserDocument {
    name?: string;
    displayName?: string;
    deadManSwitch?: DeadManSwitch;
    legacyGuardian?: LegacyGuardian;
}

interface FailedEmailRecord {
    userId: string;
    guardianEmail: string;
    userName: string;
    pinHint?: string;
    error: string;
    failedAt: number;
    retryCount: number;
    maxRetries: number;
    scheduledRunTime: string | number;
}

interface AuditLogEntry {
    eventType: string;
    userId: string;
    guardianEmail: string;
    userName: string;
    sentAt: number;
    scheduledRunTime: string | number;
    emailProvider: string;
    hasPinHint: boolean;
    functionExecutionId: string;
    inactivityDays: number;
}

// ═══════════════════════════════════════════════════════════════════════════
// FUNCIÓN AUXILIAR: DESENCRIPTACIÓN DE PIN HINT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Desencripta la pista del PIN usando AES-256-CBC.
 * 
 * ALGORITMO:
 * - Clave: SHA-256(creatorUid) → 32 bytes
 * - Algoritmo: AES-256-CBC
 * - IV: Vector de inicialización único (16 bytes)
 * 
 * COMPATIBILIDAD CON ANDROID:
 * - Android encripta con: Cipher.getInstance("AES/CBC/PKCS5Padding")
 * - Node.js desencripta con: crypto.createDecipheriv("aes-256-cbc", ...)
 * 
 * @param encryptedBase64 Texto encriptado en formato Base64
 * @param ivBase64 Vector de inicialización en formato Base64
 * @param uid UID del usuario creador (usado para derivar la clave)
 * @returns Texto plano desencriptado, o undefined si falla
 */
function decryptPinHint(
    encryptedBase64: string,
    ivBase64: string,
    uid: string
): string | undefined {
    try {
        // ═══════════════════════════════════════════════════════════════════
        // PASO 1: DERIVAR CLAVE DESDE UID (SHA-256)
        // ═══════════════════════════════════════════════════════════════════
        const key = crypto.createHash("sha256")
            .update(uid, "utf8")
            .digest();  // 32 bytes (256 bits)

        logger.debug(`[Crypto] Clave derivada desde UID: ${uid.substring(0, 8)}...`);

        // ═══════════════════════════════════════════════════════════════════
        // PASO 2: DECODIFICAR BASE64 → BUFFER
        // ═══════════════════════════════════════════════════════════════════
        const encryptedBuffer = Buffer.from(encryptedBase64, "base64");
        const ivBuffer = Buffer.from(ivBase64, "base64");

        if (ivBuffer.length !== 16) {
            throw new Error(`IV inválido: debe tener 16 bytes, recibido ${ivBuffer.length} bytes`);
        }

        // ═══════════════════════════════════════════════════════════════════
        // PASO 3: CREAR DECIPHER Y DESENCRIPTAR
        // ═══════════════════════════════════════════════════════════════════
        const decipher = crypto.createDecipheriv("aes-256-cbc", key, ivBuffer);

        let decrypted = decipher.update(encryptedBuffer, undefined, "utf8");
        decrypted += decipher.final("utf8");

        logger.info(`[Crypto] ✅ PIN Hint desencriptado exitosamente (longitud: ${decrypted.length} caracteres)`);

        return decrypted;

    } catch (error: any) {
        // ⚠️ ERROR DE DESENCRIPTACIÓN (no debe detener el cron job)
        logger.error(`[Crypto] ❌ Error desencriptando PIN Hint para UID ${uid}:`, {
            errorMessage: error.message,
            errorName: error.name,
            stack: error.stack
        });

        return undefined;
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FUNCIÓN PRINCIPAL: DEAD MAN'S SWITCH CHECKER
// ═══════════════════════════════════════════════════════════════════════════

export const checkDeadManSwitches = onSchedule(
    {
        schedule: "0 0 * * *",
        timeZone: "America/Guayaquil",
        timeoutSeconds: FUNCTION_TIMEOUT,
        memory: FUNCTION_MEMORY,
        maxInstances: 1,
        secrets: [RESEND_API_KEY]
    },
    async (event) => {
        const executionId = `exec-${Date.now()}-${randomUUID().slice(0, 8)}`;
        const scheduledRunTime = event.scheduleTime || new Date().toISOString();
        const now = Date.now();

        logger.info("═".repeat(40));
        logger.info("🔒 DEAD MAN'S SWITCH - Ejecución Iniciada", {
            executionId,
            scheduledRunTime,
            timestamp: new Date(now).toISOString(),
            timezone: "America/Guayaquil"
        });
        logger.info("═".repeat(40));

        try {
            let lastDoc: admin.firestore.QueryDocumentSnapshot | null = null;
            let totalProcessed = 0;
            let totalNotified = 0;
            let totalFailed = 0;
            let totalSkipped = 0;

            do {
                let query = db
                    .collection("users")
                    .where("deadManSwitch.active", "==", true)
                    .limit(PAGE_SIZE);

                if (lastDoc) {
                    query = query.startAfter(lastDoc);
                }

                const usersSnapshot = await query.get();

                if (usersSnapshot.empty) {
                    if (totalProcessed === 0) {
                        logger.info("ℹ️  No hay usuarios con Dead Man's Switch activo");
                    }
                    break;
                }

                logger.info(`📄 Procesando lote de ${usersSnapshot.size} usuarios`);

                const auditRecords: AuditLogEntry[] = [];

                for (const doc of usersSnapshot.docs) {
                    totalProcessed++;
                    const userData = doc.data() as UserDocument;
                    const deadManSwitch = userData.deadManSwitch;
                    const legacyGuardian = userData.legacyGuardian;

                    if (!deadManSwitch || !legacyGuardian) {
                        totalSkipped++;
                        continue;
                    }

                    const {
                        inactivityThresholdDays,
                        lastProofOfLife,
                        notificationSentAt,
                    } = deadManSwitch;

                    // ✅ EXTRAER DATOS ENCRIPTADOS
                    const guardianEmail = legacyGuardian.email;
                    const encryptedPinHint = legacyGuardian.encryptedPinHint;
                    const pinHintIv = legacyGuardian.pinHintIv;

                    if (
                        notificationSentAt ||
                        !inactivityThresholdDays ||
                        !lastProofOfLife ||
                        !guardianEmail
                    ) {
                        totalSkipped++;
                        continue;
                    }

                    const thresholdMs = inactivityThresholdDays * 24 * 60 * 60 * 1000;
                    const limitDate = lastProofOfLife + thresholdMs;
                    const daysSinceLastActivity = Math.floor((now - lastProofOfLife) / (24 * 60 * 60 * 1000));

                    if (now >= limitDate) {
                        const userName = userData.name || userData.displayName || "Usuario Anónimo";
                        const docRef = db.collection("users").doc(doc.id);

                        // ✅ DESENCRIPTAR PIN HINT
                        let decryptedPinHint: string | undefined;

                        if (encryptedPinHint && pinHintIv) {
                            logger.info(`🔓 Desencriptando PIN Hint para usuario ${doc.id}...`);
                            decryptedPinHint = decryptPinHint(encryptedPinHint, pinHintIv, doc.id);

                            if (!decryptedPinHint) {
                                logger.warn(`⚠️  No se pudo desencriptar PIN Hint para usuario ${doc.id}`);
                            }
                        } else {
                            logger.info(`ℹ️  Usuario ${doc.id} no tiene PIN Hint configurado`);
                        }

                        logger.info(`⚠️  Umbral alcanzado para usuario ${doc.id}`, {
                            userName,
                            guardianEmail,
                            daysSinceLastActivity,
                            thresholdDays: inactivityThresholdDays,
                            hasPinHint: !!decryptedPinHint
                        });

                        let transactionSuccess = false;

                        try {
                            await db.runTransaction(async (transaction) => {
                                const freshDoc = await transaction.get(docRef);

                                if (!freshDoc.exists) {
                                    logger.warn(`⚠️  Usuario ${doc.id} eliminado durante procesamiento`);
                                    return;
                                }

                                const freshData = freshDoc.data() as UserDocument;
                                const freshSwitch = freshData?.deadManSwitch;

                                if (freshSwitch?.notificationSentAt || freshSwitch?.processingStartedAt) {
                                    logger.warn(`⚠️  Usuario ${doc.id} ya procesado`);
                                    totalSkipped++;
                                    return;
                                }

                                const invitationRef = db.collection("legacyInvitations").doc(doc.id);
                                const existingInvitation = await transaction.get(invitationRef);

                                if (existingInvitation.exists) {
                                    logger.warn(`⚠️  Invitación ya existe para ${doc.id}`);
                                    totalSkipped++;
                                    return;
                                }

                                transaction.update(docRef, {
                                    "deadManSwitch.processingStartedAt": now,
                                    "deadManSwitch.processingExecutionId": executionId,
                                    "deadManSwitch.unlockStatus": "UNSEALED"
                                });

                                // ✅ GUARDAR PIN HINT DESENCRIPTADO EN INVITATION
                                const invitationData = {
                                    creatorUid: doc.id,
                                    creatorName: userName,
                                    guardianEmail: guardianEmail.toLowerCase(),
                                    pinHint: decryptedPinHint || null,  // ✅ Guardar pista desencriptada
                                    status: "PENDING",
                                    createdAt: admin.firestore.FieldValue.serverTimestamp(),
                                    vaultContentRef: `users/${doc.id}`,
                                    failedAttempts: 0,
                                    inactivityDays: daysSinceLastActivity,
                                    thresholdDays: inactivityThresholdDays,
                                    lastProofOfLife: lastProofOfLife,
                                    functionExecutionId: executionId
                                };

                                transaction.set(invitationRef, invitationData);

                                logger.info(`✅ Invitación creada con PIN Hint desencriptado`);

                                transactionSuccess = true;
                            });

                            if (!transactionSuccess) {
                                continue;
                            }

                            // ✅ ENVIAR EMAIL CON PIN HINT DESENCRIPTADO
                            try {
                                await sendGuardianEmail(guardianEmail, userName, decryptedPinHint);

                                logger.info(`✅ Email enviado exitosamente para ${doc.id}`);

                                await docRef.update({
                                    "deadManSwitch.notificationSentAt": now,
                                    "deadManSwitch.processingStartedAt": admin.firestore.FieldValue.delete(),
                                    "deadManSwitch.processingExecutionId": admin.firestore.FieldValue.delete()
                                });

                                totalNotified++;

                                auditRecords.push({
                                    eventType: "DEAD_MAN_SWITCH_TRIGGERED",
                                    userId: doc.id,
                                    guardianEmail,
                                    userName,
                                    sentAt: now,
                                    scheduledRunTime,
                                    emailProvider: EMAIL_PROVIDER,
                                    hasPinHint: !!decryptedPinHint,
                                    functionExecutionId: executionId,
                                    inactivityDays: daysSinceLastActivity
                                });

                            } catch (emailError: any) {
                                logger.error(`❌ Error enviando email para ${doc.id}:`, emailError);

                                totalFailed++;

                                await docRef.update({
                                    "deadManSwitch.processingStartedAt": admin.firestore.FieldValue.delete(),
                                    "deadManSwitch.processingExecutionId": admin.firestore.FieldValue.delete(),
                                    "deadManSwitch.lastEmailError": emailError.message || "Unknown error",
                                    "deadManSwitch.lastEmailErrorAt": now
                                });

                                const failedEmailRecord: FailedEmailRecord = {
                                    userId: doc.id,
                                    guardianEmail,
                                    userName,
                                    pinHint: decryptedPinHint,  // ✅ Guardar pista desencriptada
                                    error: emailError.message || "Unknown error",
                                    failedAt: now,
                                    retryCount: 0,
                                    maxRetries: MAX_EMAIL_RETRIES,
                                    scheduledRunTime
                                };

                                await db.collection("_failedEmails").add(failedEmailRecord);
                                logger.info(`📥 Registro añadido a DLQ para ${doc.id}`);
                            }

                        } catch (transactionError: any) {
                            logger.error(`❌ Error en transacción para ${doc.id}:`, transactionError);
                            totalFailed++;
                        }
                    }
                }

                if (auditRecords.length > 0) {
                    logger.info(`📝 Escribiendo ${auditRecords.length} registros de auditoría...`);

                    for (let i = 0; i < auditRecords.length; i += BATCH_SIZE) {
                        const batch = db.batch();
                        const chunk = auditRecords.slice(i, i + BATCH_SIZE);

                        chunk.forEach((record) => {
                            const auditRef = db.collection("_auditLog").doc();
                            batch.set(auditRef, record);
                        });

                        await batch.commit();
                    }

                    logger.info(`✅ Audit logs escritos`);
                }

                lastDoc = usersSnapshot.docs[usersSnapshot.docs.length - 1];

            } while (lastDoc);

            const executionTimeSec = ((Date.now() - now) / 1000).toFixed(2);

            logger.info("═".repeat(40));
            logger.info("✅ EJECUCIÓN COMPLETADA", {
                totalProcessed,
                totalNotified,
                totalFailed,
                totalSkipped,
                executionTimeSeconds: executionTimeSec
            });
            logger.info("═".repeat(40));

        } catch (error: any) {
            logger.error("💥 ERROR CRÍTICO:", error);
            throw error;
        }
    }
);

// ═══════════════════════════════════════════════════════════════════════════
// RETRY MECHANISM (SIN CAMBIOS SIGNIFICATIVOS)
// ═══════════════════════════════════════════════════════════════════════════

export const retryFailedEmails = onSchedule(
    {
        schedule: "0 * * * *",
        timeZone: "America/Guayaquil",
        timeoutSeconds: 300,
        memory: "512MiB",
        maxInstances: 1,
        secrets: [RESEND_API_KEY]
    },
    async (event) => {
        const now = Date.now();
        const executionId = `retry-${now}-${randomUUID().slice(0, 8)}`;

        logger.info("🔄 RETRY MECHANISM - Procesando DLQ...");

        try {
            const failedEmailsSnapshot = await db
                .collection("_failedEmails")
                .where("retryCount", "<", MAX_EMAIL_RETRIES)
                .limit(100)
                .get();

            if (failedEmailsSnapshot.empty) {
                logger.info("✅ No hay emails fallidos pendientes");
                return;
            }

            logger.info(`📥 Encontrados ${failedEmailsSnapshot.size} emails para reintentar`);

            let retriedCount = 0;
            let successCount = 0;
            let exhaustedCount = 0;

            for (const doc of failedEmailsSnapshot.docs) {
                const record = doc.data() as FailedEmailRecord;
                const { userId, guardianEmail, userName, pinHint, retryCount, maxRetries } = record;

                const backoffMs = Math.pow(2, retryCount) * 60 * 60 * 1000;
                const nextRetryTime = record.failedAt + backoffMs;

                if (now < nextRetryTime) {
                    continue;
                }

                retriedCount++;
                logger.info(`🔄 Reintentando email para ${userId} (${retryCount + 1}/${maxRetries})`);

                try {
                    await sendGuardianEmail(guardianEmail, userName, pinHint);

                    logger.info(`✅ Reintento exitoso para ${userId}`);
                    successCount++;

                    await db.collection("users").doc(userId).update({
                        "deadManSwitch.notificationSentAt": now,
                        "deadManSwitch.retriedSuccessfully": true
                    });

                    await db.collection("_auditLog").add({
                        eventType: "DEAD_MAN_SWITCH_RETRY_SUCCESS",
                        userId,
                        guardianEmail,
                        userName,
                        sentAt: now,
                        scheduledRunTime: event.scheduleTime,
                        emailProvider: EMAIL_PROVIDER,
                        hasPinHint: !!pinHint,
                        functionExecutionId: executionId,
                        originalFailureAt: record.failedAt,
                        retryAttempt: retryCount + 1
                    });

                    await doc.ref.delete();

                } catch (retryError: any) {
                    logger.error(`❌ Fallo en reintento para ${userId}:`, retryError);

                    const newRetryCount = retryCount + 1;

                    if (newRetryCount >= maxRetries) {
                        logger.error(`⛔ Reintentos agotados para ${userId}`);
                        exhaustedCount++;

                        await db.collection("_permanentFailures").add({
                            ...record,
                            finalError: retryError.message,
                            exhaustedAt: now,
                            totalRetries: newRetryCount
                        });

                        await doc.ref.delete();

                    } else {
                        await doc.ref.update({
                            retryCount: newRetryCount,
                            failedAt: now,
                            error: retryError.message
                        });
                    }
                }
            }

            logger.info("═".repeat(40));
            logger.info("✅ RETRY COMPLETADO", {
                totalProcessed: retriedCount,
                successful: successCount,
                exhausted: exhaustedCount
            });
            logger.info("═".repeat(40));

        } catch (error: any) {
            logger.error("💥 ERROR EN RETRY:", error);
            throw error;
        }
    }
);

export { sendGuardianEmail, decryptPinHint };  // ✅ Exportar para tests
export { claimInheritance } from "./claimInheritance";
