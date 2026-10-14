# Lascito — guía para Claude Code

Lascito (Perennia Systems) es una app Android para preservar recuerdos (foto, video, audio con contexto) de personas y mascotas, y transmitirlos a un guardián cuando el creador deja de dar señales de vida.

## Stack
- Kotlin, Jetpack Compose, Material 3, Navigation Compose, un solo Activity (`MainActivity`).
- Firebase: Auth (email/password), Firestore, Storage. Cloud Functions en `functions/` (TypeScript, v2 scheduler).
- Media3 (ExoPlayer), Coil, DataStore, BCrypt (`at.favre.lib:bcrypt`).
- Idiomas: `values/` (es), `values-en/`, `values-pt-rBR/`. Siempre mantener paridad de claves.

## Comandos
- Compilar: `./gradlew assembleDebug`
- Lint: `./gradlew lint`
- Tests unitarios (cuando existan): `./gradlew testDebugUnitTest`
- Functions: `cd functions && npm run build` · desplegar: `firebase deploy --only functions`
- Reglas (cuando estén versionadas): `firebase deploy --only firestore:rules,storage`

Después de cada cambio: compilar. Si falla, arreglar antes de seguir.

## Glosario (obligatorio en código nuevo y en renombres)
| UI (es) | Código | Nota |
|---|---|---|
| Legado | `Legacy` | Lo que el usuario crea para alguien. Reemplaza `Altar` y `Profile`. |
| Recuerdo | `Memory` | Pertenece a un Legado. |
| Guardián | `Guardian` | Persona designada para heredar. |
| Herencia | `Inheritance` | Lo que el guardián reclama. NO usar `Legacy` para esto (`LegacyVault`, `LegacyInvitation`, `ClaimLegacy` deben migrar a `Inheritance*`). |
| Cápsula | `Capsule` | Mensaje con fecha o condición de apertura. |
| Prueba de vida | `ProofOfLife` | Señal que reinicia el Dead Man's Switch. |

## Arquitectura
```
app/                raíz de la UI (LascitoAppRoot, AppViewModel); MainActivity y LascitoApp en el paquete base
core/designsystem/  theme/ (color, tipografía, acento) y component/ (LoadingOverlay, SectionHeader, Lascito*)
core/media/         selector de medios (MediaSourceSheet), grabadora, compresor, reproductores;
                    único punto de creación de ExoPlayer: rememberExoPlayer
core/common/        ErrorHandler, ValidationRules
data/               repositorios (interfaces + Firebase*/DataStore*), modelos, SessionRepository,
                    PreferencesRepository, error/ (DataErrors), di/ (módulos Hilt)
feature/auth/  feature/legacy/  feature/memory/  feature/custody/  feature/settings/
navigation/         NavGraph y rutas
```
Pendiente: tokens de forma, espacio y movimiento en `core/designsystem`.
Cada pantalla: `XRoute` (obtiene ViewModel, recoge estado) + `XContent` (sin ViewModel, con `@Preview`).

## Reglas
- Ningún archivo fuera de `data/` importa `com.google.firebase.*`. La sesión se lee de `SessionRepository.authState`.
- Los ViewModels reciben repositorios por constructor (Hilt). Nunca `= FirebaseXRepository()` como default.
- Nada de `Color(0x...)`, `RoundedCornerShape(n.dp)` ni duraciones de animación sueltas en pantallas: usar tokens.
- Todo texto visible va en `strings.xml` de los 3 idiomas.
- Sin `Log.*` en código de producción.
- Archivos de UI por debajo de ~300 líneas; extraer secciones a archivos propios.
- Seguridad: la verificación del PIN de herencia y el conteo de intentos ocurren en una Cloud Function `onCall`; el cliente nunca lee `accessPinHash` ni escribe `status`/`failedAttempts`.
- No afirmar "Zero-Knowledge" ni "E2E" en código, comentarios o README salvo que el contenido esté cifrado con una clave que el servidor no pueda derivar.
- Las reglas de Firestore y Storage viven en el repo (`firestore.rules`, `storage.rules`) y se despliegan desde ahí.

## Movimiento (identidad "santuario")
Lento, suave, con peso. Sin rebotes exagerados.
- Duraciones: corta 220 ms, media 360 ms, larga 520 ms.
- Transiciones de navegación: fundido + escala leve, definidas una vez en el `NavHost`.
- Retrato del legado: elemento compartido Dashboard → cabecera del legado.
- Haptics en: guardar recuerdo, desbloquear, confirmar acciones destructivas.

## Forma de trabajar
- Cambios grandes: primero plan, luego implementación por pasos pequeños que compilan.
- Un PR por fase del refactor; no mezclar refactor con cambios visuales.
- Antes de renombrar, buscar todos los usos (incluidas rutas de Firestore/Storage: los nombres de colecciones existentes NO se renombran sin migración).
