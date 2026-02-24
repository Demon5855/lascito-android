package com.perenniasystems.lascito.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.model.ProfileStatus
import com.perenniasystems.lascito.data.model.ProfileType
import com.perenniasystems.lascito.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Inmersive altar card — cuadrícula 2 columnas.
 *
 * Layout (vertical):
 *   ┌─────────────────┐
 *   │                 │  ← imagen ocupa mitad superior (~160dp)
 *   │   [Foto]        │
 *   │                 │
 *   ├── degradado ────┤
 *   │ Nombre completo │  ← serif, blanco cálido
 *   │ Parentesco      │  ← gris secundario
 *   │ 1940 — 2001     │  ← dorado apagado, sin palabras extra
 *   └─────────────────┘
 */
@Composable
fun ProfileCard(
    profile:  Profile,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit = {}
) {
    val cardShape = RoundedCornerShape(16.dp)
    val yearFmt   = SimpleDateFormat("yyyy", Locale.getDefault())
    val dateFmt   = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Formato de fechas: año solo si han pasado más de 2 años, si no fecha completa
    val birthYear    = yearFmt.format(Date(profile.birthDate))
    val passingLabel = profile.passingDate?.let { yearFmt.format(Date(it)) } ?: "Presente"
    val dateRange    = "$birthYear — $passingLabel"

    Card(
        shape    = cardShape,
        colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
        onClick   = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .border(width = 0.5.dp, color = CardBorder, shape = cardShape)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Imagen — mitad superior ────────────────────────────────────────
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profile.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${profile.fullName}",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)     // 60% de la tarjeta
                    .align(Alignment.TopCenter)
            )

            // Placeholder si no hay imagen (URL vacía o carga fallida)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.TopCenter)
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = if (profile.profileType == ProfileType.PET) "🐾" else "✦",
                    style = MaterialTheme.typography.displayMedium.copy(color = MutedGoldDim)
                )
            }

            // ── Degradado de imagen → fondo ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.6f to Color.Transparent,
                                1.0f to SurfaceDark
                            )
                        )
                    )
            )

            // ── Texto — parte inferior ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Nombre (serif)
                Text(
                    text      = profile.fullName,
                    style     = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlayfairDisplay,
                        color      = TextPrimary
                    ),
                    textAlign = TextAlign.Center,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Parentesco
                Text(
                    text  = profile.relationship,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    textAlign = TextAlign.Center,
                    maxLines  = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Fechas limpias
                Text(
                    text  = dateRange,
                    style = MaterialTheme.typography.bodySmall.copy(color = MutedGoldDim),
                    textAlign = TextAlign.Center
                )
            }

            // ── Badge archivado (READ_ONLY) ────────────────────────────────────
            if (profile.status == ProfileStatus.READ_ONLY) {
                Text(
                    text     = "⚯",
                    style    = MaterialTheme.typography.bodySmall.copy(color = MutedGoldDim),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun ProfileCardPreview() {
    LascitoTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileCard(
                profile  = Profile(
                    profileId     = "1",
                    creatorUserId = "u1",
                    fullName      = "Elena Ríos Marchetti",
                    profileType   = ProfileType.HUMAN,
                    relationship  = "Abuela",
                    birthDate     = 599616000000L,
                    passingDate   = 1672531200000L,
                    avatarUrl     = "",
                    status        = ProfileStatus.ACTIVE
                ),
                modifier = Modifier.weight(1f)
            )
            ProfileCard(
                profile  = Profile(
                    profileId     = "2",
                    creatorUserId = "u1",
                    fullName      = "Simba",
                    profileType   = ProfileType.PET,
                    relationship  = "Mi perro",
                    birthDate     = 1483228800000L,
                    passingDate   = null,
                    avatarUrl     = "",
                    status        = ProfileStatus.ACTIVE
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
