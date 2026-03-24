package org.xplore.project.ui.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import org.jetbrains.compose.resources.stringResource
import org.xplore.project.domain.model.SavedRoute
import org.xplore.project.domain.model.SavedRouteWaypoint
import org.xplore.project.ui.util.buildStaticMapUrl
import xploreapp.composeapp.generated.resources.*

/**
 * Reusable section displaying a horizontal list of route cards.
 * Used in Profile for both saved and completed routes.
 */
@Composable
fun RouteListSection(
    title: String,
    routes: List<SavedRoute>,
    emptyMessage: String,
    onShareRoute: (SavedRoute) -> Unit,
    onDeleteRoute: ((SavedRoute) -> Unit)? = null,
    showOpenButton: Boolean = false,
    onOpenRoute: ((SavedRoute) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Map,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (routes.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(routes, key = { it.id }) { route ->
                    RouteCard(
                        route = route,
                        onShare = { onShareRoute(route) },
                        onDelete = onDeleteRoute?.let { { it(route) } },
                        showOpenButton = showOpenButton,
                        onOpen = onOpenRoute?.let { { it(route) } },
                    )
                }
            }
        }
    }
}

/**
 * Route preview: OSM static map tiles as background + Canvas polyline overlay.
 * Falls back to dark Canvas-only if the image fails to load.
 */
@Composable
private fun RoutePreview(
    waypoints: List<SavedRouteWaypoint>,
    modifier: Modifier = Modifier,
) {
    val mapUrl = buildStaticMapUrl(waypoints)

    Box(modifier = modifier) {
        if (mapUrl != null) {
            SubcomposeAsyncImage(
                model = mapUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    // Dark background while loading
                    Canvas(Modifier.fillMaxSize()) { drawRect(Color(0xFF1A1A2E)) }
                },
                error = {
                    Canvas(Modifier.fillMaxSize()) { drawRect(Color(0xFF1A1A2E)) }
                },
            )
        } else {
            Canvas(Modifier.fillMaxSize()) { drawRect(Color(0xFF1A1A2E)) }
        }
        // Polyline overlay (transparent background, drawn on top of map tiles)
        RoutePolylineOverlay(waypoints = waypoints, modifier = Modifier.fillMaxSize())
    }
}

/**
 * Draws a route polyline + waypoint dots on a transparent Canvas.
 * Meant to be layered on top of a map image.
 */
@Composable
private fun RoutePolylineOverlay(
    waypoints: List<SavedRouteWaypoint>,
    modifier: Modifier = Modifier,
) {
    val routeBlue = Color(0xFF4A90D9)
    Canvas(modifier = modifier) {
        if (waypoints.size < 2) return@Canvas
        val sorted = waypoints.sortedBy { it.orderIndex }
        val minLat = sorted.minOf { it.latitude }
        val maxLat = sorted.maxOf { it.latitude }
        val minLon = sorted.minOf { it.longitude }
        val maxLon = sorted.maxOf { it.longitude }
        val latRange = (maxLat - minLat).takeIf { it > 0.0 } ?: 0.001
        val lonRange = (maxLon - minLon).takeIf { it > 0.0 } ?: 0.001
        val pad = size.minDimension * 0.15f
        val dW = size.width - 2 * pad
        val dH = size.height - 2 * pad

        fun pt(lat: Double, lon: Double) = Offset(
            x = (pad + ((lon - minLon) / lonRange * dW)).toFloat(),
            y = (pad + ((1.0 - (lat - minLat) / latRange) * dH)).toFloat(),
        )

        // Draw route line with shadow for visibility on map tiles
        val path = Path()
        val first = pt(sorted[0].latitude, sorted[0].longitude)
        path.moveTo(first.x, first.y)
        sorted.drop(1).forEach { wp ->
            val o = pt(wp.latitude, wp.longitude)
            path.lineTo(o.x, o.y)
        }
        // Shadow stroke for contrast
        drawPath(
            path = path,
            color = Color.Black.copy(alpha = 0.4f),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        // Main stroke
        drawPath(
            path = path,
            color = routeBlue,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        // Waypoint dots
        sorted.forEach { wp ->
            val o = pt(wp.latitude, wp.longitude)
            drawCircle(color = Color.Black.copy(alpha = 0.3f), radius = 5.dp.toPx(), center = o)
            drawCircle(color = routeBlue, radius = 4.dp.toPx(), center = o)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = o)
        }
    }
}

@Composable
private fun RouteCard(
    route: SavedRoute,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?,
    showOpenButton: Boolean = false,
    onOpen: (() -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ),
        modifier = Modifier.width(220.dp),
    ) {
        Column {
            if (route.waypoints.size >= 2) {
                RoutePreview(
                    waypoints = route.waypoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!route.description.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = route.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(Res.string.route_stops_count, route.waypoints.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showOpenButton && onOpen != null) {
                        Button(
                            onClick = onOpen,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4A90D9),
                            ),
                        ) {
                            Text(
                                text = stringResource(Res.string.route_open_action),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    } else {
                        Spacer(Modifier.width(0.dp))
                    }

                    Row {
                        IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(Res.string.route_share_action),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        if (onDelete != null) {
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(Res.string.route_delete_action),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
