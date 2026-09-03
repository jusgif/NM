package com.nuvio.app.features.streams

import kotlin.test.Test
import kotlin.test.assertEquals

class SmartStreamSelectorTest {
    private fun stream(
        name: String,
        addonId: String = "addon.test",
        url: String? = "https://cdn.example.com/video.mkv",
        resolution: String? = null,
        cached: Boolean = false,
        notWebReady: Boolean = false,
    ) = StreamItem(
        name = name,
        url = url,
        addonName = "Test",
        addonId = addonId,
        behaviorHints = StreamBehaviorHints(notWebReady = notWebReady),
        clientResolve = resolution?.let {
            StreamClientResolve(
                isCached = cached,
                stream = StreamClientResolveStream(
                    raw = StreamClientResolveRaw(parsed = StreamClientResolveParsed(resolution = it))
                )
            )
        }
    )

    @Test
    fun `ranks higher quality when no constraints are supplied`() {
        val low = stream("720p", resolution = "720p")
        val high = stream("1080p", resolution = "1080p")
        assertEquals(listOf(high, low), SmartStreamSelector.rank(listOf(low, high)))
    }

    @Test
    fun `prefers cached debrid over otherwise equal direct stream`() {
        val direct = stream("1080p", resolution = "1080p")
        val cached = stream("1080p cached", addonId = "addon.debrid", resolution = "1080p", cached = true)
        assertEquals(cached, SmartStreamSelector.rank(listOf(direct, cached)).first())
    }

    @Test
    fun `penalizes non web ready streams`() {
        val normal = stream("1080p", resolution = "1080p")
        val notReady = stream("1080p", resolution = "1080p", notWebReady = true)
        assertEquals(normal, SmartStreamSelector.rank(listOf(notReady, normal)).first())
    }

    @Test
    fun `uses display height to avoid unnecessary 4k selection`() {
        val hd = stream("1080p", resolution = "1080p")
        val uhd = stream("2160p", resolution = "2160p")
        val ranked = SmartStreamSelector.rank(
            listOf(uhd, hd),
            SmartStreamSelector.Context(displayHeight = 1080)
        )
        assertEquals(hd, ranked.first())
    }

    @Test
    fun `ranking remains deterministic for equal scores`() {
        val first = stream("1080p A", resolution = "1080p")
        val second = stream("1080p B", resolution = "1080p")
        assertEquals(listOf(first, second), SmartStreamSelector.rank(listOf(first, second)))
    }
}
