/**
 * Correo de aviso al guardián cuando se activa el Dead Man's Switch.
 *
 * Se envía con la API HTTP de Resend. El correo nunca contiene el PIN: solo
 * la pista que dejó el creador.
 *
 * Configuración:
 * - RESEND_API_KEY (secreto):  firebase functions:secrets:set RESEND_API_KEY
 * - EMAIL_FROM (parámetro):    remitente verificado en Resend. El valor por
 *   defecto es el remitente de pruebas de Resend, que solo entrega al correo
 *   del dueño de la cuenta de Resend.
 */

import * as logger from "firebase-functions/logger";
import { defineSecret, defineString } from "firebase-functions/params";

export const EMAIL_PROVIDER = "resend";
export const RESEND_API_KEY = defineSecret("RESEND_API_KEY");
const EMAIL_FROM = defineString("EMAIL_FROM", {
    default: "Lascito <onboarding@resend.dev>",
    description: "Remitente de los correos al guardián (dominio verificado en Resend)"
});

const RESEND_ENDPOINT = "https://api.resend.com/emails";

/** La pista y los nombres los escribe el usuario: se escapan antes de ir al HTML. */
function escapeHtml(value: string): string {
    return value
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function buildText(guardianEmail: string, userName: string, pinHint?: string): string {
    const hint = pinHint ?
        `Pista que dejó ${userName}: "${pinHint}"` :
        `${userName} no dejó una pista para el PIN.`;

    return [
        `${userName} te designó guardián de su legado en Lascito.`,
        "",
        `Como ${userName} lleva un tiempo sin dar señales de vida, su legado ya está disponible para ti.`,
        "",
        "Cómo acceder:",
        `1. Abre la app Lascito e inicia sesión (o crea una cuenta) con este correo: ${guardianEmail}`,
        "2. Verifica tu correo si la app te lo pide.",
        `3. En la pantalla de inicio verás el aviso del legado de ${userName}. Pulsa "Reclamar Bóveda".`,
        `4. Escribe el PIN de 4 dígitos que ${userName} te compartió en persona.`,
        "",
        hint,
        "",
        "Tienes 3 intentos para escribir el PIN. Al tercer fallo el legado queda bloqueado.",
        "Este correo no contiene el PIN.",
        "",
        "Si no esperabas este mensaje, puedes ignorarlo."
    ].join("\n");
}

function buildHtml(guardianEmail: string, userName: string, pinHint?: string): string {
    const name = escapeHtml(userName);
    const email = escapeHtml(guardianEmail);
    const hintBlock = pinHint ?
        `<div style="border:1px solid #C9A96E;border-radius:12px;padding:20px;margin:24px 0;">
           <p style="margin:0 0 8px 0;color:#C9A96E;font-size:13px;letter-spacing:1px;text-transform:uppercase;">Pista que dejó ${name}</p>
           <p style="margin:0;color:#F5F5F5;font-size:17px;font-style:italic;line-height:1.6;">&ldquo;${escapeHtml(pinHint)}&rdquo;</p>
         </div>` :
        `<p style="color:#BBBBBB;margin:24px 0;">${name} no dejó una pista para el PIN.</p>`;

    return `<!DOCTYPE html>
<html lang="es">
<head><meta charset="utf-8"></head>
<body style="margin:0;padding:0;background:#0D0D0D;color:#E8E8E8;font-family:Georgia,'Times New Roman',serif;">
  <div style="max-width:560px;margin:0 auto;padding:40px 24px;">
    <p style="color:#C9A96E;font-size:26px;letter-spacing:2px;text-align:center;margin:0 0 32px 0;">LASCITO</p>
    <div style="background:#1A1A1A;border:1px solid #2E2E2E;border-radius:16px;padding:32px;">
      <h1 style="color:#C9A96E;font-size:22px;margin:0 0 16px 0;">${name} te dejó un legado</h1>
      <p style="line-height:1.7;">${name} te designó guardián de su legado en Lascito. Como lleva un tiempo sin dar señales de vida, su legado ya está disponible para ti.</p>
      <h2 style="font-size:17px;margin:28px 0 12px 0;">Cómo acceder</h2>
      <ol style="line-height:1.8;padding-left:22px;color:#CCCCCC;">
        <li>Abre la app Lascito e inicia sesión (o crea una cuenta) con este correo: <strong style="color:#C9A96E;">${email}</strong></li>
        <li>Verifica tu correo si la app te lo pide.</li>
        <li>En la pantalla de inicio verás el aviso del legado de ${name}. Pulsa <strong>Reclamar Bóveda</strong>.</li>
        <li>Escribe el PIN de 4 dígitos que ${name} te compartió en persona.</li>
      </ol>
      ${hintBlock}
      <p style="color:#BBBBBB;font-size:14px;line-height:1.6;border-left:3px solid #C9A96E;padding-left:14px;">Tienes 3 intentos para escribir el PIN. Al tercer fallo el legado queda bloqueado. Este correo no contiene el PIN.</p>
    </div>
    <p style="color:#777777;font-size:12px;text-align:center;margin-top:28px;">Si no esperabas este mensaje, puedes ignorarlo.</p>
  </div>
</body>
</html>`;
}

/**
 * Envía el aviso al guardián.
 *
 * @param guardianEmail Email del guardián
 * @param userName Nombre del creador del legado
 * @param decryptedPinHint Pista del PIN en claro (opcional)
 * @throws Error si Resend rechaza el envío; quien llama lo registra para reintento
 */
export async function sendGuardianEmail(
    guardianEmail: string,
    userName: string,
    decryptedPinHint?: string
): Promise<void> {
    const response = await fetch(RESEND_ENDPOINT, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${RESEND_API_KEY.value()}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            from: EMAIL_FROM.value(),
            to: [guardianEmail],
            subject: `Lascito: ${userName} te dejó un legado`,
            html: buildHtml(guardianEmail, userName, decryptedPinHint),
            text: buildText(guardianEmail, userName, decryptedPinHint)
        })
    });

    if (!response.ok) {
        const body = await response.text();
        throw new Error(`Resend API error ${response.status}: ${body.slice(0, 300)}`);
    }

    const data = await response.json() as { id?: string };
    logger.info("[Email Service] Aviso al guardián enviado", {
        provider: EMAIL_PROVIDER,
        messageId: data.id ?? null,
        hasPinHint: !!decryptedPinHint
    });
}
