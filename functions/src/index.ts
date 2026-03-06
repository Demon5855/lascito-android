/**
 * ═══════════════════════════════════════════════════════════════════════════
 * LASCITO - DEAD MAN'S SWITCH CLOUD FUNCTION (Production Grade)
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Función programada que verifica diariamente el estado de inactividad de los 
 * usuarios y notifica a sus herederos cuando se alcanza el umbral configurado.
 * 
 * CARACTERÍSTICAS DE PRODUCCIÓN:
 * - Paginación cursor-based (evita OOM con grandes volúmenes)
 * - Transacciones atómicas (previene race conditions)
 * - Dead Letter Queue para reintentos automáticos
 * - Audit Log completo para compliance
 * - Batching de escrituras respetando límites de Firestore
 * - Manejo robusto de errores por documento
 * - Timezone explícito (America/Guayaquil)
 * - Memoria y timeout configurados
 * 
 * @version 2.0.0
 * @author Lascito Engineering Team
 * @lastModified 2026-03-05
 */

import { onSchedule } from "firebase-functions/v2/scheduler";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import { randomUUID } from "crypto";

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
const PAGE_SIZE = 500;           // Documentos por iteración de paginación
const BATCH_SIZE = 500;          // Límite de Firestore por batch (máximo absoluto)
const MAX_EMAIL_RETRIES = 3;     // Reintentos máximos en Dead Letter Queue
const FUNCTION_TIMEOUT = 540;    // 9 minutos (máximo para Gen 2)
const FUNCTION_MEMORY = "1GiB";  // Memoria asignada a la función

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
    accessPinHash?: string | null;
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
    unlockPinHash?: string;
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
    unlockMethod: string;
    functionExecutionId: string;
    inactivityDays: number;
}

// ═══════════════════════════════════════════════════════════════════════════
// SERVICIO DE ENVÍO DE EMAILS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Envía el correo de notificación al heredero/guardian.
 * 
 * 🔧 PRODUCCIÓN: Reemplazar con integración real (Resend, SendGrid, SES, etc.)
 * 
 * OPCIONES DE INTEGRACIÓN:
 * 
 * 1. RESEND (Recomendado para Firebase):
 *    ```typescript
 *    import { Resend } from 'resend';
 *    const resend = new Resend(process.env.RESEND_API_KEY);
 *    await resend.emails.send({
 *      from: 'noreply@lascito.app',
 *      to: guardianEmail,
 *      subject: 'Acceso de Emergencia - Lascito',
 *      html: `<p>La bóveda de ${userName} está disponible...</p>`
 *    });
 *    ```
 * 
 * 2. SENDGRID:
 *    ```typescript
 *    import sgMail from '@sendgrid/mail';
 *    sgMail.setApiKey(process.env.SENDGRID_API_KEY!);
 *    await sgMail.send({ from, to, subject, html });
 *    ```
 * 
 * 3. AWS SES (via AWS SDK):
 *    ```typescript
 *    import { SESClient, SendEmailCommand } from '@aws-sdk/client-ses';
 *    const ses = new SESClient({ region: 'us-east-1' });
 *    await ses.send(new SendEmailCommand({ ... }));
 *    ```
 * 
 * @param guardianEmail Email del heredero
 * @param userName Nombre del usuario fallecido/inactivo
 * @param unlockPinHash Hash del PIN de desbloqueo (opcional)
 * @returns Promise que resuelve cuando el email se envía exitosamente
 * @throws Error si el proveedor de email falla
 */
async function sendGuardianEmail(
    guardianEmail: string,
    userName: string,
    unlockPinHash: string | undefined
): Promise<void> {
    // ═══════════════════════════════════════════════════════════════════════════
    // MOCK DE ENVÍO DE EMAIL (TEMPORAL)
    // ═══════════════════════════════════════════════════════════════════════════
    logger.info(`[Email Service] Preparando notificación confidencial`, {
        recipient: guardianEmail,
        userName,
        hasPinHash: !!unlockPinHash,
        provider: "MOCK (Development Mode)"
    });

    // Simulación de latencia de red (200-500ms)
    const latency = 200 + Math.random() * 300;
    await new Promise((resolve) => setTimeout(resolve, latency));

    // Simulación aleatoria de fallo (5% de probabilidad para testing de DLQ)
    if (Math.random() < 0.05) {
        throw new Error("Email provider rate limit exceeded (simulated failure)");
    }

    logger.info(`[Email Service] ✅ Notificación enviada exitosamente a ${guardianEmail}`);

    // ═══════════════════════════════════════════════════════════════════════════
    // REEMPLAZAR EN PRODUCCIÓN CON CÓDIGO REAL:
    // ═══════════════════════════════════════════════════════════════════════════
    /*
    const emailProvider = process.env.EMAIL_PROVIDER || "resend"; // resend | sendgrid | ses
    
    if (emailProvider === "resend") {
      const { Resend } = await import("resend");
      const resend = new Resend(process.env.RESEND_API_KEY);
      
      const { data, error } = await resend.emails.send({
        from: "Lascito <noreply@lascito.app>",
        to: guardianEmail,
        subject: `🔓 Acceso de Emergencia Activado - Bóveda de ${userName}`,
        html: `
          <!DOCTYPE html>
          <html>
          <head>
            <meta charset="utf-8">
            <style>
              body { font-family: 'Playfair Display', serif; background: #0D0D0D; color: #E8E8E8; }
              .container { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
              .header { text-align: center; margin-bottom: 40px; }
              .logo { color: #D4AF37; font-size: 28px; font-weight: 600; }
              .content { background: #1A1A1A; border: 1px solid #2A2A2A; border-radius: 12px; padding: 30px; }
              .pin-box { background: #252525; border: 2px solid #D4AF37; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center; }
              .pin-code { font-size: 32px; font-weight: 700; color: #D4AF37; letter-spacing: 4px; }
              .footer { margin-top: 30px; text-align: center; color: #666; font-size: 12px; }
            </style>
          </head>
          <body>
            <div class="container">
              <div class="header">
                <div class="logo">⚱️ LASCITO</div>
                <p>Protocolo de Emergencia Activado</p>
              </div>
              
              <div class="content">
                <h2>Acceso a la Bóveda Digital</h2>
                <p>Estimado Heredero/Guardian,</p>
                <p>El protocolo de inactividad (Dead Man's Switch) ha sido activado para la cuenta de <strong>${userName}</strong>.</p>
                <p>De acuerdo con las instrucciones dejadas por el titular, ahora tienes acceso completo a su bóveda digital de memorias y documentos.</p>
                
                ${unlockPinHash ? `
                  <div class="pin-box">
                    <p style="margin: 0 0 10px 0; color: #D4AF37; font-weight: 600;">PIN DE DESBLOQUEO</p>
                    <div class="pin-code">${unlockPinHash}</div>
                    <p style="margin: 10px 0 0 0; font-size: 11px; color: #888;">Este PIN es de un solo uso y expira en 30 días</p>
                  </div>
                ` : `
                  <p><strong>Método de Acceso:</strong> Biométrico (configurado en el dispositivo del titular)</p>
                `}
                
                <h3>Próximos Pasos</h3>
                <ol>
                  <li>Descarga la aplicación Lascito en tu dispositivo móvil</li>
                  <li>Selecciona "Acceso de Heredero" en la pantalla inicial</li>
                  <li>Ingresa el email asociado: <strong>${guardianEmail}</strong></li>
                  ${unlockPinHash ? '<li>Introduce el PIN de desbloqueo mostrado arriba</li>' : ''}
                  <li>Sigue las instrucciones en pantalla para verificar tu identidad</li>
                </ol>
                
                <p style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #2A2A2A; color: #888; font-size: 13px;">
                  Este correo fue generado automáticamente por el sistema de seguridad de Lascito. 
                  Si crees que esto es un error, contacta inmediatamente a soporte@lascito.app
                </p>
              </div>
              
              <div class="footer">
                <p>© 2026 Lascito Digital Vault · Todos los derechos reservados</p>
                <p>Este es un correo confidencial. No lo compartas con terceros.</p>
              </div>
            </div>
          </body>
          </html>
        `
      });
      
      if (error) {
        throw new Error(`Resend API Error: ${error.message}`);
      }
      
      logger.info(`[Resend] Email entregado con ID: ${data?.id}`);
    }
    
    // Agregar más providers aquí (SendGrid, SES, etc.)
    */
}

// ═══════════════════════════════════════════════════════════════════════════
// FUNCIÓN PRINCIPAL: DEAD MAN'S SWITCH CHECKER
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Cloud Function programada que se ejecuta diariamente para verificar
 * el estado de inactividad de los usuarios y activar notificaciones a herederos.
 * 
 * SCHEDULE: Todos los días a las 00:00 (medianoche) en timezone de Ecuador
 * TIMEOUT: 9 minutos (máximo para Gen 2)
 * MEMORY: 1 GiB
 * CONCURRENCIA: 1 instancia (previene race conditions)
 */
export const checkDeadManSwitches = onSchedule(
    {
        schedule: "0 0 * * *",
        timeZone: "America/Guayaquil",
        timeoutSeconds: FUNCTION_TIMEOUT,
        memory: FUNCTION_MEMORY,
        maxInstances: 1
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
            // ═══════════════════════════════════════════════════════════════════
            // FASE 1: PAGINACIÓN CURSOR-BASED
            // ═══════════════════════════════════════════════════════════════════
            let lastDoc: admin.firestore.QueryDocumentSnapshot | null = null;
            let totalProcessed = 0;
            let totalNotified = 0;
            let totalFailed = 0;
            let totalSkipped = 0;

            do {
                // Construir query con paginación
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
                        logger.info("ℹ️  No hay usuarios con Dead Man's Switch activo en el sistema");
                    }
                    break;
                }

                logger.info(`📄 Procesando lote de ${usersSnapshot.size} usuarios (Total acumulado: ${totalProcessed + usersSnapshot.size})`);

                // ═════════════════════════════════════════════════════════════════
                // FASE 2: PROCESAMIENTO TRANSACCIONAL POR DOCUMENTO
                // ═════════════════════════════════════════════════════════════════
                const auditRecords: AuditLogEntry[] = [];

                for (const doc of usersSnapshot.docs) {
                    totalProcessed++;
                    const userData = doc.data() as UserDocument;
                    const deadManSwitch = userData.deadManSwitch;

                    const legacyGuardian = userData.legacyGuardian;

                    // Validación de estructura de datos
                    if (!deadManSwitch || !legacyGuardian) {
                        totalSkipped++;
                        continue;
                    }

                    const {
                        inactivityThresholdDays,
                        lastProofOfLife,
                        notificationSentAt,
                    } = deadManSwitch;

                    // Extraemos los datos del Guardián desde su propio objeto
                    const guardianEmail = legacyGuardian.email;
                    const unlockPinHash = legacyGuardian.accessPinHash || undefined;

                    // Skip si ya fue notificado o le faltan datos críticos
                    if (
                        notificationSentAt ||
                        !inactivityThresholdDays ||
                        !lastProofOfLife ||
                        !guardianEmail
                    ) {
                        totalSkipped++;
                        continue;
                    }

                    // ═══════════════════════════════════════════════════════════════
                    // CÁLCULO DE THRESHOLD DE INACTIVIDAD
                    // ═══════════════════════════════════════════════════════════════
                    const thresholdMs = inactivityThresholdDays * 24 * 60 * 60 * 1000;
                    const limitDate = lastProofOfLife + thresholdMs;
                    const daysSinceLastActivity = Math.floor((now - lastProofOfLife) / (24 * 60 * 60 * 1000));

                    // ¿Ha superado el umbral de inactividad?
                    if (now >= limitDate) {
                        const userName = userData.name || userData.displayName || "Usuario Anónimo";
                        const docRef = db.collection("users").doc(doc.id);

                        logger.info(`⚠️  Umbral alcanzado para usuario ${doc.id}`, {
                            userName,
                            guardianEmail,
                            daysSinceLastActivity,
                            thresholdDays: inactivityThresholdDays
                        });

                        // ═════════════════════════════════════════════════════════════
                        // TRANSACCIÓN ATÓMICA: MARCAR COMO "PROCESANDO"
                        // ═════════════════════════════════════════════════════════════
                        let transactionSuccess = false;

                        try {
                            await db.runTransaction(async (transaction) => {
                                const freshDoc = await transaction.get(docRef);

                                if (!freshDoc.exists) {
                                    logger.warn(`⚠️  Usuario ${doc.id} fue eliminado durante el procesamiento`);
                                    return;
                                }

                                const freshData = freshDoc.data() as UserDocument;
                                const freshSwitch = freshData?.deadManSwitch;

                                // Double-check: prevenir race condition con otra instancia
                                if (freshSwitch?.notificationSentAt || freshSwitch?.processingStartedAt) {
                                    logger.warn(`⚠️  Usuario ${doc.id} ya está siendo procesado por otra instancia`);
                                    totalSkipped++;
                                    return;
                                }

                                // Marcar como "procesando" con timestamp
                                transaction.update(docRef, {
                                    "deadManSwitch.processingStartedAt": now,
                                    "deadManSwitch.processingExecutionId": executionId
                                });

                                transactionSuccess = true;
                            });

                            if (!transactionSuccess) {
                                continue; // Skip al siguiente usuario
                            }

                            // ═══════════════════════════════════════════════════════════
                            // ENVÍO DE EMAIL AL HEREDERO
                            // ═══════════════════════════════════════════════════════════
                            try {
                                await sendGuardianEmail(guardianEmail, userName, unlockPinHash);

                                // ✅ EMAIL ENVIADO EXITOSAMENTE
                                logger.info(`✅ Email enviado exitosamente para usuario ${doc.id}`);

                                // Marcar como completado en la DB
                                await docRef.update({
                                    "deadManSwitch.notificationSentAt": now,
                                    "deadManSwitch.processingStartedAt": admin.firestore.FieldValue.delete(),
                                    "deadManSwitch.processingExecutionId": admin.firestore.FieldValue.delete()
                                });

                                totalNotified++;

                                // Registrar en Audit Log
                                auditRecords.push({
                                    eventType: "DEAD_MAN_SWITCH_TRIGGERED",
                                    userId: doc.id,
                                    guardianEmail,
                                    userName,
                                    sentAt: now,
                                    scheduledRunTime,
                                    emailProvider: "MOCK", // Cambiar en producción
                                    unlockMethod: unlockPinHash ? "PIN_HASH" : "BIOMETRIC_ONLY",
                                    functionExecutionId: executionId,
                                    inactivityDays: daysSinceLastActivity
                                });

                            } catch (emailError: any) {
                                // ❌ ERROR AL ENVIAR EMAIL
                                logger.error(`❌ Error enviando email para usuario ${doc.id}:`, emailError);

                                totalFailed++;

                                // Revertir el flag de "procesando"
                                await docRef.update({
                                    "deadManSwitch.processingStartedAt": admin.firestore.FieldValue.delete(),
                                    "deadManSwitch.processingExecutionId": admin.firestore.FieldValue.delete(),
                                    "deadManSwitch.lastEmailError": emailError.message || "Unknown error",
                                    "deadManSwitch.lastEmailErrorAt": now
                                });

                                // ═════════════════════════════════════════════════════════
                                // DEAD LETTER QUEUE: Guardar para reintento
                                // ═════════════════════════════════════════════════════════
                                const failedEmailRecord: FailedEmailRecord = {
                                    userId: doc.id,
                                    guardianEmail,
                                    userName,
                                    unlockPinHash,
                                    error: emailError.message || "Unknown error",
                                    failedAt: now,
                                    retryCount: 0,
                                    maxRetries: MAX_EMAIL_RETRIES,
                                    scheduledRunTime
                                };

                                await db.collection("_failedEmails").add(failedEmailRecord);
                                logger.info(`📥 Registro añadido a Dead Letter Queue para usuario ${doc.id}`);
                            }

                        } catch (transactionError: any) {
                            logger.error(`❌ Error en transacción para usuario ${doc.id}:`, transactionError);
                            totalFailed++;
                        }
                    }
                }

                // ═════════════════════════════════════════════════════════════════
                // FASE 3: BATCH WRITE DE AUDIT LOGS
                // ═════════════════════════════════════════════════════════════════
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

                    logger.info(`✅ Audit logs escritos exitosamente`);
                }

                // Preparar siguiente iteración
                lastDoc = usersSnapshot.docs[usersSnapshot.docs.length - 1];

            } while (lastDoc);

            // ═══════════════════════════════════════════════════════════════════
            // RESUMEN FINAL DE EJECUCIÓN
            // ═══════════════════════════════════════════════════════════════════
            const executionTimeMs = Date.now() - now;
            const executionTimeSec = (executionTimeMs / 1000).toFixed(2);

            logger.info("═".repeat(40));
            logger.info("✅ EJECUCIÓN COMPLETADA", {
                totalProcessed,
                totalNotified,
                totalFailed,
                totalSkipped,
                executionTimeSeconds: executionTimeSec,
                successRate: totalProcessed > 0 ? `${((totalNotified / totalProcessed) * 100).toFixed(1)}%` : "N/A"
            });
            logger.info("═".repeat(40));

        } catch (error: any) {
            logger.error("💥 ERROR CRÍTICO EN EJECUCIÓN DE DEAD MAN'S SWITCH:", error);

            // Log adicional para debugging
            logger.error("Stack trace:", error.stack);

            throw error; // Re-throw para que Firebase marque la ejecución como fallida
        }
    }
);

// ═══════════════════════════════════════════════════════════════════════════
// FUNCIÓN AUXILIAR: PROCESADOR DE DEAD LETTER QUEUE (RETRY MECHANISM)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Cloud Function que procesa emails fallidos cada hora.
 * Reintenta envíos hasta MAX_EMAIL_RETRIES veces con backoff exponencial.
 * 
 * SCHEDULE: Cada hora
 * TIMEOUT: 5 minutos
 * MEMORY: 512 MiB
 */
export const retryFailedEmails = onSchedule(
    {
        schedule: "0 * * * *",                  // Cada hora en punto
        timeZone: "America/Guayaquil",
        timeoutSeconds: 300,                    // 5 minutos
        memory: "512MiB",
        maxInstances: 1
    },
    async (event) => {
        const now = Date.now();
        const executionId = `retry-${now}-${randomUUID().slice(0, 8)}`;

        logger.info("🔄 RETRY MECHANISM - Procesando Dead Letter Queue...");

        try {
            // Obtener emails fallidos que aún no superan el máximo de reintentos
            const failedEmailsSnapshot = await db
                .collection("_failedEmails")
                .where("retryCount", "<", MAX_EMAIL_RETRIES)
                .limit(100)  // Procesar máximo 100 por hora
                .get();

            if (failedEmailsSnapshot.empty) {
                logger.info("✅ No hay emails fallidos pendientes de reintento");
                return;
            }

            logger.info(`📥 Encontrados ${failedEmailsSnapshot.size} emails para reintentar`);

            let retriedCount = 0;
            let successCount = 0;
            let exhaustedCount = 0;

            for (const doc of failedEmailsSnapshot.docs) {
                const record = doc.data() as FailedEmailRecord;
                const { userId, guardianEmail, userName, unlockPinHash, retryCount, maxRetries } = record;

                // Backoff exponencial: esperar 2^retryCount horas desde el último intento
                const backoffMs = Math.pow(2, retryCount) * 60 * 60 * 1000;
                const nextRetryTime = record.failedAt + backoffMs;

                if (now < nextRetryTime) {
                    continue; // Aún no es tiempo de reintentar
                }

                retriedCount++;
                logger.info(`🔄 Reintentando email para usuario ${userId} (Intento ${retryCount + 1}/${maxRetries})`);

                try {
                    // Intentar enviar email nuevamente
                    await sendGuardianEmail(guardianEmail, userName, unlockPinHash);

                    // ✅ ÉXITO
                    logger.info(`✅ Reintento exitoso para usuario ${userId}`);
                    successCount++;

                    // Actualizar documento de usuario
                    await db.collection("users").doc(userId).update({
                        "deadManSwitch.notificationSentAt": now,
                        "deadManSwitch.retriedSuccessfully": true
                    });

                    // Registrar en audit log
                    await db.collection("_auditLog").add({
                        eventType: "DEAD_MAN_SWITCH_RETRY_SUCCESS",
                        userId,
                        guardianEmail,
                        userName,
                        sentAt: now,
                        scheduledRunTime: event.scheduleTime,
                        emailProvider: "MOCK",
                        unlockMethod: unlockPinHash ? "PIN_HASH" : "BIOMETRIC_ONLY",
                        functionExecutionId: executionId,
                        originalFailureAt: record.failedAt,
                        retryAttempt: retryCount + 1
                    });

                    // Eliminar de la cola de fallos
                    await doc.ref.delete();

                } catch (retryError: any) {
                    logger.error(`❌ Fallo en reintento para usuario ${userId}:`, retryError);

                    const newRetryCount = retryCount + 1;

                    if (newRetryCount >= maxRetries) {
                        // ❌ AGOTADOS LOS REINTENTOS
                        logger.error(`⛔ Reintentos agotados para usuario ${userId}. Marcando como fallido permanentemente.`);
                        exhaustedCount++;

                        // Mover a colección de fallos permanentes
                        await db.collection("_permanentFailures").add({
                            ...record,
                            finalError: retryError.message,
                            exhaustedAt: now,
                            totalRetries: newRetryCount
                        });

                        // Eliminar de la cola de reintentos
                        await doc.ref.delete();

                        // Notificar al equipo de soporte (implementar webhook/alert aquí)
                        logger.error(`🚨 ALERTA: Email permanentemente fallido para ${userId}. Requiere intervención manual.`);

                    } else {
                        // Actualizar contador de reintentos
                        await doc.ref.update({
                            retryCount: newRetryCount,
                            failedAt: now,
                            error: retryError.message
                        });
                    }
                }
            }

            logger.info("═".repeat(40));
            logger.info("✅ RETRY MECHANISM COMPLETADO", {
                totalProcessed: retriedCount,
                successful: successCount,
                exhausted: exhaustedCount,
                stillPending: retriedCount - successCount - exhaustedCount
            });
            logger.info("═".repeat(40));

        } catch (error: any) {
            logger.error("💥 ERROR CRÍTICO EN RETRY MECHANISM:", error);
            throw error;
        }
    }
);

// ═══════════════════════════════════════════════════════════════════════════
// EXPORTS ADICIONALES (Para testing y monitoring)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Función HTTP para forzar ejecución manual del Dead Man's Switch.
 * Solo disponible en ambiente de desarrollo.
 * 
 * USO: POST https://us-central1-[PROJECT-ID].cloudfunctions.net/manualTriggerDeadManSwitch
 * HEADERS: { "X-Admin-Token": "tu-token-secreto" }
 */
export { sendGuardianEmail }; // Exportar para tests unitarios
