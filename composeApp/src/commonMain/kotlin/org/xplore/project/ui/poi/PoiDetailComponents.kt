package org.xplore.project.ui.poi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource

import org.xplore.project.ui.components.pinTypeColor
import xploreapp.composeapp.generated.resources.*

/**
 * Reusable UI components for the POI Detail screen.
 */

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = Color(0xFF888888),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    )
}

@Composable
fun InfoCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        content()
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color(0xFF333333),
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F2F5)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF666666),
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color(0xFF888888),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 14.sp,
                fontWeight = if (onClick != null) FontWeight.SemiBold else FontWeight.Normal,
                modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
            )
        }
    }
}

@Composable
fun HeroImage(pin: org.xplore.project.domain.model.MapPin) {
    if (!pin.imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = pin.imageUrl,
            contentDescription = pin.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color(0xFFEEEEEE)),
        )
    }
}

@Composable
fun PoiDetailBody(
    pin: org.xplore.project.domain.model.MapPin,
    notAvailable: String,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    onRateClick: () -> Unit,
    onMarkVisitedClick: () -> Unit,
    visitStatus: VisitStatus,
    isGuest: Boolean,
) {
    Column(modifier = Modifier.padding(20.dp)) {
        // Category badge
        if (!pin.category.isNullOrBlank()) {
            val accentColor = pinTypeColor(pin.type)
            Text(
                text = pin.category.uppercase(),
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(12.dp))
        }

        // Name
        Text(
            text = pin.label,
            color = Color(0xFF1A1A2E),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp,
        )

        Spacer(Modifier.height(12.dp))

        // Rating & Add Stop Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRateClick)
                    .background(Color(0xFFF0F5FA))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Vota",
                    tint = Color(0xFF0050A0),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                
                if (pin.rating != null && pin.rating > 0) {
                    Text(
                        text = pin.rating.toString(),
                        color = Color(0xFF0050A0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (pin.ratingsCount == 1) 
                                    stringResource(Res.string.poi_rating_count_format_single)
                                else 
                                    stringResource(Res.string.poi_rating_count_format, pin.ratingsCount ?: 0),
                        color = Color(0xFF555555),
                        fontSize = 13.sp
                    )
                    if (isGuest) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.poi_rating_login_prompt),
                            color = Color(0xFF0050A0),
                            fontSize = 13.sp,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                } else {
                    Text(
                        text = if (isGuest) stringResource(Res.string.poi_rating_login_prompt) else "Vota per primo!",
                        color = Color(0xFF0050A0),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }

            // Mark as Visited Button
            Button(
                onClick = onMarkVisitedClick,
                shape = RoundedCornerShape(8.dp),
                enabled = visitStatus !is VisitStatus.Loading && visitStatus !is VisitStatus.Success,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90D9),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                if (visitStatus is VisitStatus.Loading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp), 
                        color = Color.White, 
                        strokeWidth = 2.dp
                    )
                } else if (visitStatus is VisitStatus.Success) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Visitato",
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.competition_mark_visited),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Description
        if (!pin.description.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            SectionHeader(stringResource(Res.string.poi_detail_description))
            Spacer(Modifier.height(6.dp))
            Text(
                text = pin.description,
                color = Color(0xFF444444),
                fontSize = 14.sp,
                lineHeight = 22.sp,
            )
        }

        Spacer(Modifier.height(20.dp))

        // Info Card
        InfoCard {
            InfoRow(
                icon = Icons.Filled.AccessTime,
                label = stringResource(Res.string.poi_detail_opening_hours),
                value = pin.openingHours ?: notAvailable,
                valueColor = if (pin.openingHours != null) Color(0xFF333333) else Color(0xFFAAAAAA),
            )
            InfoRow(
                icon = Icons.Filled.AttachMoney,
                label = stringResource(Res.string.poi_detail_cost),
                value = formatFee(fee = pin.fee, notAvailable = notAvailable),
                valueColor = feeColor(pin.fee),
            )
            if (!pin.website.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.Filled.Language,
                    label = stringResource(Res.string.poi_detail_website),
                    value = pin.website.removePrefix("https://").removePrefix("http://").removeSuffix("/"),
                    valueColor = Color(0xFF4A90D9),
                    onClick = {
                        val url = if (pin.website.startsWith("http")) pin.website else "https://${pin.website}"
                        uriHandler.openUri(url)
                    },
                )
            }
            if (!pin.phone.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.Filled.Phone,
                    label = stringResource(Res.string.poi_detail_phone),
                    value = pin.phone,
                    valueColor = Color(0xFF4A90D9),
                    onClick = { uriHandler.openUri("tel:${pin.phone}") },
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun formatFee(fee: String?, notAvailable: String): String = when {
    fee == null -> notAvailable
    fee.equals("no", ignoreCase = true) -> stringResource(Res.string.poi_detail_free)
    fee.equals("yes", ignoreCase = true) -> stringResource(Res.string.poi_detail_paid)
    else -> fee
}

fun feeColor(fee: String?): Color = when {
    fee == null -> Color(0xFFAAAAAA)
    fee.equals("no", ignoreCase = true) -> Color(0xFF388E3C)
    else -> Color(0xFF333333)
}
