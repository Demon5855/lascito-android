# Lascito — Android MVP (Phase 1)
> **Perennia Systems** · Package: `com.perenniasystems.lascito`

A digital mausoleum for preserving memories of loved ones — built with Android Kotlin, Jetpack Compose, and a full MVVM architecture.

---

## 🚀 Quick Start

### 1. Open in Android Studio

Open `c:\Santuario` as an **existing Android project** in Android Studio **Ladybug (2024.2.1)** or newer. Let Gradle sync complete.

### 2. Add the Custom Fonts (Required before compiling)

The theme uses **Playfair Display** (serif) and **Lato** (sans-serif). Download the `.ttf` files and place them in:

```
app/src/main/res/font/
```

Required filenames (must match exactly):

| File | Download |
|------|----------|
| `playfair_display_regular.ttf` | [Google Fonts → Playfair Display](https://fonts.google.com/specimen/Playfair+Display) — Regular 400 |
| `playfair_display_italic.ttf` | Same page — Italic 400 |
| `playfair_display_bold.ttf` | Same page — Bold 700 |
| `lato_regular.ttf` | [Google Fonts → Lato](https://fonts.google.com/specimen/Lato) — Regular 400 |
| `lato_light.ttf` | Same page — Light 300 |
| `lato_bold.ttf` | Same page — Bold 700 |

> **Tip:** On Google Fonts, click "Download family", unzip, and rename the files to match the table above.

### 3. Run

- Target: **API 26+** emulator (prefer Pixel series with OLED display skin) or a physical device.
- Press ▶ **Run** in Android Studio.

---

## 📂 Project Structure

```
app/src/main/java/com/perenniasystems/lascito/
├── MainActivity.kt                    ← Single-activity shell
├── navigation/
│   ├── Screen.kt                      ← Type-safe route objects
│   └── NavGraph.kt                    ← Compose NavHost
├── data/
│   ├── model/
│   │   ├── Profile.kt                 ← Profile data class + enums
│   │   └── Memory.kt                  ← Memory data class + enums
│   └── repository/
│       ├── ProfileRepository.kt       ← Interface (DI contract)
│       └── MockProfileRepository.kt   ← Phase 1 mock data (4 profiles)
├── viewmodel/
│   └── DashboardViewModel.kt          ← StateFlow UI state
└── ui/
    ├── theme/
    │   ├── Color.kt                   ← AMOLED palette
    │   ├── Type.kt                    ← Playfair Display + Lato
    │   └── Theme.kt                   ← Dark-only MaterialTheme
    ├── components/
    │   └── LascitoComponents.kt       ← Shared TextField, Buttons
    └── screens/
        ├── auth/
        │   ├── LoginScreen.kt
        │   └── RegisterScreen.kt
        └── dashboard/
            ├── DashboardScreen.kt
            └── ProfileCard.kt
schemas/
├── profile_schema.json                ← JSON Schema Draft-07
└── memory_schema.json
```

---

## 🗂 Data Schemas

See [`schemas/profile_schema.json`](schemas/profile_schema.json) and [`schemas/memory_schema.json`](schemas/memory_schema.json) for the canonical NoSQL document structures used for Firestore/DynamoDB in Phase 2.

---

## 🔮 Phase 2 Roadmap

| Feature | Tech |
|---------|------|
| Real Authentication | Firebase Authentication |
| Profile storage | Firestore (`profiles/{id}`) |
| Memory metadata | Firestore (`profiles/{id}/memories/{id}`) |
| Media upload | AWS S3 via Lambda presigned URLs |
| Long-term archival | S3 Glacier Deep Archive lifecycle rules |

---

## 🏗 Architecture

- **Pattern:** MVVM (Model → ViewModel → UI)
- **UI:** Jetpack Compose (dark-only)
- **State:** `StateFlow<DashboardUiState>` (sealed interface: Loading / Success / Error)
- **DI:** Constructor injection — swap `MockProfileRepository` for any `ProfileRepository` implementation with zero ViewModel changes
- **Navigation:** Compose Navigation (type-safe `Screen` sealed class)
