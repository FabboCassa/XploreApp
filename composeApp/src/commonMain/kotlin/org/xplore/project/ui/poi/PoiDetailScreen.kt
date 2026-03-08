package org.xplore.project.ui.poi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.xplore.project.ui.components.pinTypeColor
import xploreapp.composeapp.generated.resources.Res
import xploreapp.composeapp.generated.resources.poi_detail_back
import xploreapp.composeapp.generated.resources.poi_detail_cost
import xploreapp.composeapp.generated.resources.poi_detail_description
import xploreapp.composeapp.generated.resources.poi_detail_free
import xploreapp.composeapp.generated.resources.poi_detail_info_not_available
import xploreapp.composeapp.generated.resources.poi_detail_opening_hours
import xploreapp.composeapp.generated.resources.poi_detail_paid
import xploreapp.composeapp.generated.resources.poi_detail_phone
import xploreapp.composeapp.generated.resources.poi_detail_website
import xploreapp.composeapp.generated.resources.poi_rating_count_format
import xploreapp.composeapp.generated.resources.poi_rating_count_format_single
import xploreapp.composeapp.generated.resources.poi_no_rating
import xploreapp.composeapp.generated.resources.poi_add_stop
import xploreapp.composeapp.generated.resources.competition_mark_visited
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import xploreapp.composeapp.generated.resources.poi_rating_login_prompt

/**
 * Full-screen POI detail page.
 *
 * Shows hero image, category, name, description, opening hours, cost,
 * and contact information for a Point of Interest.
 *
 * Sub-components (SectionHeader, InfoCard, InfoRow) live in PoiDetailComponents.kt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailScreen(
    poiId: String,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    viewModel: PoiDetailViewModel = koinViewModel(),
) {
    LaunchedEffect(poiId) { viewModel.loadPin(poiId) }

    val pin by viewModel.pin.collectAsState()
    val uriHandler = LocalUriHandler.current
    val notAvailable = stringResource(Res.string.poi_detail_info_not_available)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pin?.label ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.poi_detail_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A1A2E),
                    navigationIconContentColor = Color(0xFF1A1A2E),
                ),
            )
        },
        containerColor = Color(0xFFF8F8FA),
        contentWindowInsets = WindowInsets.systemBars,
    ) { innerPadding ->
        val currentPin = pin

        if (currentPin == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = notAvailable, color = Color(0xFF888888), fontSize = 14.sp)
            }
        } else {
            var showRatingDialog by remember { mutableStateOf(false) }
            val ratingStatus by viewModel.ratingStatus.collectAsState()

            val visitStatus by viewModel.visitStatus.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            ) {
                HeroImage(currentPin)
                PoiDetailBody(
                    pin = currentPin,
                    notAvailable = notAvailable,
                    uriHandler = uriHandler,
                    onRateClick = { 
                        if (viewModel.isGuest) {
                            onNavigateToLogin()
                        } else {
                            viewModel.resetRatingStatus()
                            showRatingDialog = true 
                        }
                    },
                    onMarkVisitedClick = {
                        if (viewModel.isGuest) {
                            onNavigateToLogin()
                        } else {
                            viewModel.markAsVisited(currentPin.id)
                        }
                    },
                    visitStatus = visitStatus,
                    isGuest = viewModel.isGuest,
                )
            }

            if (showRatingDialog) {
                RatingDialog(
                    poiName = currentPin.label,
                    status = ratingStatus,
                    onDismiss = { showRatingDialog = false },
                    onSubmit = { score -> viewModel.submitRating(currentPin.id, score) }
                )
            }
        }
    }
}


