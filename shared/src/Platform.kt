package org.baconana.mybeautifulgit_chan

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
