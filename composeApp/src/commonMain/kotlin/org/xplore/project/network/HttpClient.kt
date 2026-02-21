package org.xplore.project.network

import io.ktor.client.HttpClient

/**
 * Creates a platform-specific HttpClient.
 * Android will use OkHttp and bypass SSL for localhost.
 * iOS will use Darwin.
 */
expect fun createHttpClient(): HttpClient
