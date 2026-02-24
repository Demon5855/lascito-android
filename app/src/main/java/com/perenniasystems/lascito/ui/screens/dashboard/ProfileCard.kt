package com.perenniasystems.lascito.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.perenniasystems.lascito.R
import com.perenniasystems.lascito.data.model.Profile
import com.perenniasystems.lascito.data.model.ProfileStatus
import com.perenniasystems.lascito.data.model.ProfileType
import com.perenniasystems.lascito.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Visual card representing a single [Profile] in the Dashboard list.
 *
 * Layout:
 *   ┌────────────────────────────────────────┐
 *   │ [Avatar]  Full Name          ✦ Tag     │
 *   │           Relationship                 │
 *   │           Born: dd/MM/yyyy             │
 *   │           Passed: dd/MM/yyyy | Presente│
 *   └────────────────────────────────────────┘
 */
@Composable
fun ProfileCard(
    profile: Profile,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = CardBorder,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Avatar ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, MutedGold, CircleShape)
                    .background(SurfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profile.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${profile.fullName} avatar",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )

                // Pet badge
                if (profile.profileType == ProfileType.PET) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .background(MutedGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐾", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // ── Text block ────────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text     = profile.fullName,
                        style    = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // Status badge
                    if (profile.status == ProfileStatus.READ_ONLY) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text  = "⚯",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutedGoldDim)
                        )
                    }
                }

                Text(
                    text  = profile.relationship,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Dates ─────────────────────────────────────────────────────
                val birthFormatted = dateFormat.format(Date(profile.birthDate))
                val passingFormatted = profile.passingDate?.let { dateFormat.format(Date(it)) }
                    ?: stringResource(R.string.label_present)

                Row {
                    DateChip(label = stringResource(R.string.label_born), date = birthFormatted)
                    Spacer(modifier = Modifier.width(8.dp))
                    DateChip(
                        label = if (profile.passingDate != null)
                            stringResource(R.string.label_passing)
                        else
                            stringResource(R.string.label_present),
                        date  = if (profile.passingDate != null) passingFormatted else "",
                        accentColor = if (profile.passingDate != null) TextSecondary else MutedGold
                    )
                }
            }
        }
    }
}

@Composable
private fun DateChip(
    label: String,
    date: String,
    accentColor: androidx.compose.ui.graphics.Color = TextSecondary
) {
    Column {
        Text(
            text  = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(color = MutedGoldDim)
        )
        Text(
            text  = date,
            style = MaterialTheme.typography.bodySmall.copy(color = accentColor)
        )
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun ProfileCardPreview() {
    LascitoTheme {
        ProfileCard(
            profile = Profile(
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
            modifier = Modifier.padding(16.dp)
        )
    }
}
