package com.perenniasystems.lascito.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.model.ProfileStatus
import com.perenniasystems.lascito.data.model.ProfileType
import com.perenniasystems.lascito.ui.theme.*
import com.perenniasystems.lascito.ui.components.shimmerEffect
import androidx.compose.ui.res.stringResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Inmersive altar card — cuadrícula 2 columnas.
 *
 * Capas (de atrás hacia adelante):
 *   1. Placeholder con emoji (PET 🐾 / HUMAN ✦) sobre fondo oscuro
 *   2. AsyncImage — tapa el placeholder cuando la foto carga
 *   3. Degradado imagen → fondo de tarjeta
 *   4. Texto (Nombre, Parentesco, Fechas)
 *   5. Badge tipo de perfil (esquina superior izquierda)
 *   6. Badge READ_ONLY (esquina superior derecha)
 */
@Composable
fun ProfileCard(
    profile:  Profile,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit = {},
    onEdit:   () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(16.dp)
    val yearFmt   = SimpleDateFormat("yyyy", Locale.getDefault())

    val birthYear    = yearFmt.format(Date(profile.birthDate))
    val passingLabel = profile.passingDate?.let { yearFmt.format(Date(it)) } ?: "Presente"
    val dateRange    = "$birthYear — $passingLabel"

    Card(
        shape    = cardShape,
        colors   = CardDefaults.cardColors(containerColor = SurfaceDark),
        onClick  = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .border(width = 0.5.dp, color = CardBorder, shape = cardShape)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Foto del perfil (con Loading Shimmer y Error state combinados) ────
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(profile.avatarUrl.takeIf { it.isNotBlank() })
                    .crossfade(true)
                    .build(),
                contentDescription = profile.fullName,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.TopCenter),
                loading = {
                    Box(modifier = Modifier.fillMaxSize().shimmerEffect())
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile.profileType == ProfileType.PET) {
                            Text(
                                text  = "🐾",
                                style = MaterialTheme.typography.displayMedium.copy(color = MaterialTheme.colorScheme.primaryContainer)
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Default.Person,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.primaryContainer,
                                modifier           = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            )

            // ── Capa 3: Degradado imagen → fondo de la tarjeta ────────────────
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

            // ── Capa 4: Texto ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                Text(
                    text  = profile.relationship,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    textAlign = TextAlign.Center,
                    maxLines  = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text  = dateRange,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primaryContainer),
                    textAlign = TextAlign.Center
                )
            }

            // ── Capa 5: Badge tipo de perfil (esquina superior izquierda) ─────
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp)
                    .size(28.dp)
                    .background(AmoledBlack.copy(alpha = 0.72f), CircleShape)
                    .border(0.8.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profile.profileType == ProfileType.PET) {
                    Text(
                        text  = "🐾",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Default.Person,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(16.dp)
                    )
                }
            }

            // ── Capa 6: Dropdown Menu & READ_ONLY Badge (esquina superior derecha) ─────
            val textEdit = stringResource(R.string.action_edit)
            val textDelete = stringResource(R.string.action_delete)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 8.dp)
            ) {
                if (profile.status == ProfileStatus.ACTIVE) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(AmoledBlack.copy(alpha = 0.72f), CircleShape)
                            .border(0.8.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .clip(CircleShape)
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            DropdownMenuItem(
                                text = { Text(textEdit, color = TextPrimary) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(textDelete, color = ErrorRed) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                } else if (profile.status == ProfileStatus.READ_ONLY) {
                    Text(
                        text     = "⚯",
                        style    = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.padding(4.dp)
                    )
                }
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
