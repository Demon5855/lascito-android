# 🏛️ Lascito — Santuario Digital Privado

> **Perennia Systems** · Package: `com.perenniasystems.lascito`  
> **Status**: ✅ Fase 4 Completada (Blindaje de Seguridad) | 🚧 Fase 5 En Desarrollo (Fideicomiso)

Una aplicación Android nativa que funciona como un **mausoleo digital premium** para preservar memorias de seres queridos, con arquitectura de seguridad bancaria y modelo Freemium.

<div align="center">

[![Android](https://img.shields.io/badge/Platform-Android%20API%2026+-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com)

</div>

---

## ✨ Características Implementadas

### 🔐 **Seguridad de Nivel Bancario (Fase 4)**
- ✅ **BiometricPrompt nativo** con período de gracia de 30s anti-bypass
- ✅ **Strict Vault Mode** (FLAG_SECURE) para prevenir screenshots/grabación
- ✅ **Email Verification obligatoria** antes de subir contenido
- ✅ **Recuperación de contraseña** con rate limiting
- ✅ **Cambio de contraseña** con reautenticación estricta
- ✅ **Token refresh** antes de operaciones críticas
- ✅ **Session expiration handling** robusto

### 🎨 **Experiencia de Usuario Premium**
- ✅ **Sistema de temas personalizables** (Gold, Silver, Bronze, Platinum, Rose Gold, Emerald)
- ✅ **Multi-idioma** (Español/Inglés) con cambio instantáneo sin recrear Activity
- ✅ **Onboarding interactivo** con indicadores de progreso
- ✅ **Snackbar centralizado** con acciones y feedback inmediato
- ✅ **Auth-Aware UI** con bienvenida personalizada
- ✅ **Loading states** con shimmer effects elegantes

### 📸 **Gestión de Memorias Multimedia**
- ✅ **Soporte completo**: Imágenes, Videos, Audio
- ✅ **Captura de fotos/videos** desde la app
- ✅ **Grabación de audio** con contador en tiempo real
- ✅ **Coil** para carga eficiente de imágenes con caché
- ✅ **ExoPlayer** para reproducción de video con controles nativos
- ✅ **CustomAudioPlayer** con waveform visual
- ✅ **Progress tracking** durante uploads largos

### 👥 **Perfiles Diferenciados**
- ✅ **Tipos de perfil**: Persona, Mascota
- ✅ **Metadatos completos**: Nombre, relación, fechas, avatar
- ✅ **CRUD completo** con validación y confirmación
- ✅ **Altares personalizados** por perfil
- ✅ **Timeline de memorias** ordenada cronológicamente

### 🔥 **Backend Firebase**
- ✅ **Firebase Authentication** (Email/Password)
- ✅ **Firestore** para perfiles y memorias
- ✅ **Firebase Storage** para media files
- ✅ **Security Rules** configuradas
- ✅ **Offline persistence** habilitada

---

## 🚀 Quick Start

### **Prerrequisitos**
- Android Studio **Ladybug (2024.2.1)** o superior
- JDK 17 o superior
- Dispositivo/Emulador con **API 26+** (Android 8.0+)
- Cuenta de Firebase configurada

---

### **1. Clonar el Repositorio**

```bash
git clone https://github.com/Demon5855/lascito-android.git
cd lascito-android
```

---

### **2. Configurar Firebase**

1. **Descargar `google-services.json`**:
   - Ve a [Firebase Console](https://console.firebase.google.com/)
   - Selecciona tu proyecto Lascito
   - Ve a **Project Settings** → **General**
   - En "Your apps", descarga `google-services.json`

2. **Colocar el archivo**:
   ```
   lascito-android/
   └── app/
       └── google-services.json  ← Aquí
   ```

3. **Habilitar servicios**:
   - **Authentication**: Email/Password provider
   - **Firestore**: Database en modo producción
   - **Storage**: Bucket con reglas de seguridad

---

### **3. Instalar Fuentes (Requerido)**

El tema usa **Playfair Display** (serif) y **Lato** (sans-serif).

**Descarga e instala**:

| Archivo | Fuente |
|---------|--------|
| `playfair_display_regular.ttf` | [Google Fonts - Playfair Display](https://fonts.google.com/specimen/Playfair+Display) (Regular 400) |
| `playfair_display_italic.ttf` | Italic 400 |
| `playfair_display_bold.ttf` | Bold 700 |
| `lato_regular.ttf` | [Google Fonts - Lato](https://fonts.google.com/specimen/Lato) (Regular 400) |
| `lato_light.ttf` | Light 300 |
| `lato_bold.ttf` | Bold 700 |

**Ubicación**:
```
app/src/main/res/font/
├── playfair_display_regular.ttf
├── playfair_display_italic.ttf
├── playfair_display_bold.ttf
├── lato_regular.ttf
├── lato_light.ttf
└── lato_bold.ttf
```

> **Tip**: En Google Fonts, descarga la familia completa, descomprime y renombra los archivos.

---

### **4. Compilar y Ejecutar**

```bash
# Desde Android Studio
./gradlew assembleDebug

# Instalar en dispositivo conectado
./gradlew installDebug

# O presiona ▶ Run en Android Studio
```

**Target**: API 26+ (recomendado: Pixel 7 emulator con OLED display)

---

## 📂 Arquitectura del Proyecto

### **Estructura de Carpetas**

```
app/src/main/java/com/perenniasystems/lascito/
├── MainActivity.kt                          ← Single-Activity con BiometricLock
│
├── navigation/
│   ├── Screen.kt                            ← Type-safe routes (sealed class)
│   └── NavGraph.kt                          ← Compose Navigation con AuthGuard
│
├── data/
│   ├── model/
│   │   ├── Profile.kt                       ← Data class + ProfileType enum
│   │   ├── Memory.kt                        ← Data class + MediaType enum
│   │   └── Guardian.kt                      ← (Fase 5) Dead Man's Switch
│   │
│   ├── repository/
│   │   ├── UserRepository.kt                ← Auth interface
│   │   ├── FirebaseAuthRepository.kt        ← Firebase Auth implementation
│   │   ├── ProfileRepository.kt             ← Profile CRUD interface
│   │   ├── FirebaseProfileRepository.kt     ← Firestore profiles implementation
│   │   ├── MemoryRepository.kt              ← Memory CRUD interface
│   │   ├── FirebaseMemoryRepository.kt      ← Firestore memories implementation
│   │   ├── FirebaseStorageRepository.kt     ← Storage upload/download
│   │   └── ThemeRepository.kt               ← DataStore theme persistence
│   │
│   └── local/
│       └── OnboardingPreferences.kt         ← DataStore (onboarding, vault mode)
│
├── viewmodel/
│   ├── AuthViewModel.kt                     ← Login/Register/Reset Password
│   ├── DashboardViewModel.kt                ← Profile list + email verification
│   ├── AltarViewModel.kt                    ← Memory list por perfil
│   ├── AddProfileViewModel.kt               ← Profile creation/edit
│   ├── AddMemoryViewModel.kt                ← Memory upload con progress
│   └── UserSettingsViewModel.kt             ← Settings + Change Password
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt                         ← AMOLED Black palette + accent metals
│   │   ├── Type.kt                          ← Playfair Display + Lato
│   │   ├── Theme.kt                         ← Dark-only Material3 theme
│   │   ├── ThemeState.kt                    ← Global theme state (Compose State)
│   │   └── LangState.kt                     ← Global language state
│   │
│   ├── components/
│   │   ├── LascitoTextField.kt              ← Custom input fields
│   │   ├── LascitoAlertDialog.kt            ← Branded dialogs
│   │   ├── CustomAudioPlayer.kt             ← Audio playback con waveform
│   │   └── ProfileCard.kt                   ← Dashboard profile cards
│   │
│   └── screens/
│       ├── onboarding/
│       │   └── OnboardingScreen.kt          ← 3-step intro
│       ├── auth/
│       │   ├── LoginScreen.kt               ← Login + Forgot Password
│       │   └── RegisterScreen.kt            ← Register con email verification
│       ├── dashboard/
│       │   └── DashboardScreen.kt           ← Home con InfoBanner
│       ├── profile/
│       │   └── AddProfileScreen.kt          ← Create/Edit profile
│       ├── altar/
│       │   └── AltarScreen.kt               ← Memory feed + ExoPlayer
│       ├── memory/
│       │   └── AddMemoryScreen.kt           ← Image/Video/Audio upload
│       └── settings/
│           └── UserSettingsScreen.kt        ← Theme, Language, Security, Account
│
└── util/
    ├── ErrorHandler.kt                      ← Firebase exception mapper
    ├── LocaleHelper.kt                      ← Instant language switching
    └── PermissionHandler.kt                 ← Runtime permissions manager
```

---

### **Patrón de Arquitectura: MVVM**

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                      │
│  • Screens con @Composable                                   │
│  • collectAsStateWithLifecycle() para StateFlows             │
│  • No business logic                                         │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel Layer                           │
│  • viewModelScope para coroutines                            │
│  • StateFlow<UiState> (sealed interface)                     │
│  • Input: fun saveProfile(), fun deleteMemory()              │
│  • Output: _uiState.value = Success/Error/Loading            │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                   Repository Layer                           │
│  • Interface (abstraction)                                   │
│  • suspend fun save(profile: Profile): Result<Unit>          │
│  • Firebase implementation con try-catch                     │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                 Data Sources (Firebase)                      │
│  • FirebaseAuth                                              │
│  • FirebaseFirestore                                         │
│  • FirebaseStorage                                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Esquema de Datos (Firestore)

### **Colección: `users/{userId}`**
```json
{
  "displayName": "Juan Pérez",
  "email": "juan@example.com",
  "createdAt": 1709089200000
}
```

### **Colección: `profiles/{profileId}`**
```json
{
  "profileId": "uuid-v4",
  "userId": "firebase-user-id",
  "fullName": "María García López",
  "relationship": "Abuela",
  "profileType": "PERSON",
  "birthDate": 631152000000,
  "passingDate": 1704067200000,
  "avatarUrl": "gs://bucket/profiles/uuid/avatar.jpg",
  "createdAt": 1709089200000
}
```

### **Colección: `memories/{memoryId}`**
```json
{
  "memoryId": "uuid-v4",
  "profileId": "profile-uuid",
  "mediaType": "IMAGE",
  "mediaUrl": "gs://bucket/memories/profile-uuid/images/memory-uuid.jpg",
  "storageClass": "STANDARD",
  "contextText": "Última Navidad juntos en casa de los abuelos.",
  "capturedDate": 1640995200000
}
```

---

## 🔐 Políticas de Seguridad (Firebase)

### **Principios de Seguridad Implementados**

**Firestore**:
- ✅ Los usuarios solo pueden leer y escribir sus propios datos en la colección `users`
- ✅ Los perfiles solo son accesibles por su propietario (validación por `userId`)
- ✅ Las memorias requieren validación cruzada: el usuario debe ser dueño del perfil asociado
- ✅ Todas las operaciones requieren autenticación (`request.auth != null`)

**Storage**:
- ✅ Los avatares de perfiles son privados por usuario (path basado en `userId`)
- ✅ Las memorias multimedia requieren email verificado (`email_verified == true`)
- ✅ Lectura permitida solo para usuarios autenticados
- ✅ Escritura bloqueada para usuarios sin verificar email

> **Nota de Seguridad**: Las reglas completas están configuradas en Firebase Console y no se exponen públicamente por razones de OpSec.

---

## 🎨 Sistema de Temas

Lascito usa un sistema de **temas metálicos premium** con persistencia en DataStore:

| Tema | Color Primario | Uso |
|------|----------------|-----|
| 🥇 **Gold** | `#C9A96E` | Default, elegancia clásica |
| 🥈 **Silver** | `#C0C0C0` | Minimalismo moderno |
| 🥉 **Bronze** | `#CD7F32` | Calidez terrestre |
| 💎 **Platinum** | `#E5E4E2` | Lujo sofisticado |
| 🌹 **Rose Gold** | `#E0BFB8` | Delicadeza femenina |
| 💚 **Emerald** | `#50C878` | Vitalidad natural |

**Cambio instantáneo**: El usuario puede cambiar el tema desde Settings sin reiniciar la app.

---

## 🌍 Internacionalización (i18n)

**Idiomas soportados**:
- 🇪🇸 **Español** (ES)
- 🇬🇧 **English** (EN)

**Cambio instantáneo**: Sistema custom con `LocaleHelper` que envuelve el contexto en un `LocaleContextWrapper`, permitiendo cambiar idioma sin recrear la Activity.

**Ubicación de strings**:
```
app/src/main/res/
├── values/
│   └── strings.xml         ← Español (default)
└── values-en/
    └── strings.xml         ← English
```

---

## 🛡️ Características de Seguridad

### **Nivel de Aplicación**
- ✅ **BiometricPrompt** con fallback a PIN/patrón del dispositivo
- ✅ **FLAG_SECURE** dinámico para prevenir screenshots/grabación
- ✅ **Session timeout** con período de gracia de 30s
- ✅ **Email verification** obligatoria antes de uploads
- ✅ **Token refresh** antes de operaciones críticas
- ✅ **Reauthentication** requerida para cambiar contraseña

### **Nivel de Backend**
- ✅ **Security Rules** estrictas en Firestore y Storage
- ✅ **Email verification** validada server-side
- ✅ **Rate limiting** client-side en reset password (1/min)
- ✅ **Timeout protection** (15s en auth, 120s en uploads)

---

## 📱 Requisitos del Sistema

| Componente | Mínimo | Recomendado |
|------------|--------|-------------|
| **Android API** | 26 (Oreo 8.0) | 34+ (Android 14+) |
| **RAM** | 2GB | 4GB+ |
| **Almacenamiento** | 100MB | 500MB+ (para media cache) |
| **Conectividad** | Wi-Fi/4G | 5G/Wi-Fi 6 |
| **Biometría** | Opcional | Huella/Rostro |
| **Pantalla** | 5" 720p | 6"+ AMOLED 1080p+ |

---

## 🚧 Roadmap

### **Fase 4: El Blindaje** ✅ COMPLETADO
- [x] Limpieza de deuda técnica (Toasts → Snackbars)
- [x] Email verification obligatoria
- [x] BiometricPrompt con período de gracia
- [x] FLAG_SECURE con toggle
- [x] Forgot Password
- [x] Change Password con reautenticación
- [x] Auth-Aware UI

### **Fase 5: El Fideicomiso** 🚧 EN DESARROLLO
- [ ] Modelo Guardian (Dead Man's Switch)
- [ ] CRUD del heredero designado
- [ ] Configuración de tiempo de inactividad (30/60/90/180 días)
- [ ] Pantalla "Mi Custodia"
- [ ] Exportación de altar completo a ZIP

### **Fase 6: El Escudo de Costos** 📅 PLANEADO
- [ ] Room Database para caché local
- [ ] Single Source of Truth (offline-first)
- [ ] Paginación de memorias (10 por página)
- [ ] Caché agresiva de imágenes con Coil
- [ ] WorkManager para sync en background

### **Fase 7: El Ecosistema Legal** 📅 PLANEADO
- [ ] Botón "Reportar uso indebido"
- [ ] Firebase App Check
- [ ] Landing page en GitHub Pages
- [ ] Políticas de privacidad y términos
- [ ] Preparación para Google Play Store

---

## 🤝 Contribución

Este es un proyecto privado de **Perennia Systems**. No se aceptan pull requests externos en esta fase.

Para consultas o reportes de bugs internos, contacta al equipo de desarrollo.

---

## 📄 Licencia

**Propietario**: Perennia Systems  
**Todos los derechos reservados** © 2026

Este código es confidencial y propiedad de Perennia Systems. Cualquier distribución, copia o uso no autorizado está estrictamente prohibido.

---

## 📞 Soporte

**Email Corporativo**: support@perenniasystems.com  
**GitHub Issues**: [lascito-android/issues](https://github.com/Demon5855/lascito-android/issues)  
**Documentación**: Ver `/docs` (próximamente)

---

## 🙏 Agradecimientos

- **Jetpack Compose** por hacer el desarrollo UI moderno y declarativo
- **Firebase** por el backend serverless robusto
- **Coil** por la carga eficiente de imágenes
- **ExoPlayer** por la reproducción de video/audio profesional
- **Google Fonts** por las tipografías Playfair Display y Lato

---

<div align="center">

**Hecho con 🖤 por Perennia Systems**

*Preservando legados para la eternidad*

</div>
