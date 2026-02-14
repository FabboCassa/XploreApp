package org.xplore.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform