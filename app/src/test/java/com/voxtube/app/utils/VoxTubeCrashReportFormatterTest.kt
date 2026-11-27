package com.voxtube.app.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.ArrayDeque

class VoxTubeCrashReportFormatterTest {
    @Test
    fun `oom report separates input state updates and final output`() {
        val report =
            VoxTubeCrashReportFormatter.build(
                VoxTubeCrashReportSnapshot(
                    timestampMs = 1_800_000_000_000L,
                    threadName = "main",
                    threadId = 1L,
                    exceptionClass = OutOfMemoryError::class.java.name,
                    exceptionMessage = "Failed to allocate",
                    stackTrace =
                        """
                        java.lang.OutOfMemoryError: Failed to allocate
                            at com.voxtube.app.player.PlayerCacheManager.updateCache(PlayerCacheManager.kt:42)
                            at com.voxtube.app.service.VideoPlayerService.onStartCommand(VideoPlayerService.kt:121)
                            at android.app.ActivityThread.handleServiceArgs(ActivityThread.java:5000)
                        """.trimIndent(),
                    deviceInfo = "Model: Pixel Test",
                    memoryInfo = "Max heap: 256 MB",
                    breadcrumbs =
                        listOf(
                            VoxTubeCrashBreadcrumb(1_799_999_990_000L, "activity", "onStop pip=false"),
                            VoxTubeCrashBreadcrumb(1_799_999_991_000L, "background-handoff", "handleBackgroundPlaybackOnStop"),
                            VoxTubeCrashBreadcrumb(1_799_999_992_000L, "video-service", "onStartCommand action=null startId=1"),
                        ),
                ),
            )

        assertThat(report).contains("Crash Input")
        assertThat(report).contains("OutOfMemoryError: true")
        assertThat(report).contains("State/Cache Updates Before Crash")
        assertThat(report).contains("background-handoff: handleBackgroundPlaybackOnStop")
        assertThat(report).contains("Final Output")
        assertThat(report).contains("Top Stack Frames (same build):")
        assertThat(report).contains("PlayerCacheManager.updateCache")
    }

    @Test
    fun `top stack frames keeps only actionable frame lines`() {
        val frames =
            VoxTubeCrashReportFormatter.topStackFrames(
                """
                java.lang.IllegalStateException: boom
                    at first.Frame.call(First.kt:1)
                    at second.Frame.call(Second.kt:2)
                Caused by: java.io.IOException: nope
                    at third.Frame.call(Third.kt:3)
                """.trimIndent(),
                limit = 3,
            )

        assertThat(frames)
            .containsExactly(
                "at first.Frame.call(First.kt:1)",
                "at second.Frame.call(Second.kt:2)",
                "Caused by: java.io.IOException: nope",
            ).inOrder()
    }

    @Test
    fun `folded breadcrumbs show their count and heap`() {
        val report =
            VoxTubeCrashReportFormatter.build(
                VoxTubeCrashReportSnapshot(
                    timestampMs = 1_800_000_000_000L,
                    threadName = "main",
                    threadId = 1L,
                    exceptionClass = OutOfMemoryError::class.java.name,
                    exceptionMessage = null,
                    stackTrace = "java.lang.OutOfMemoryError",
                    deviceInfo = "",
                    memoryInfo = "",
                    breadcrumbs =
                        listOf(
                            VoxTubeCrashBreadcrumb(
                                1_799_999_990_000L,
                                "video-service",
                                "onStartCommand action=null",
                                heapMb = 212,
                                nativeMb = 40,
                                count = 5,
                            ),
                            VoxTubeCrashBreadcrumb(1_799_999_991_000L, "video-item", "load abc audioOnly=false"),
                        ),
                ),
            )

        assertThat(report).contains("video-service: onStartCommand action=null ×5 heap=212MB native=40MB")
        assertThat(report).contains("video-item: load abc audioOnly=false\n")
    }

    @Test
    fun `a repeated breadcrumb folds into one and the ring keeps its size`() {
        val ring = ArrayDeque<VoxTubeCrashBreadcrumb>()
        ring.record(VoxTubeCrashBreadcrumb(1, "activity", "onResume"), max = 3)
        repeat(
            5,
        ) { ring.record(VoxTubeCrashBreadcrumb(2L + it, "video-service", "onStartCommand action=null", heapMb = 100L + it), max = 3) }
        ring.record(VoxTubeCrashBreadcrumb(10, "video-item", "load a"), max = 3)
        ring.record(VoxTubeCrashBreadcrumb(11, "video-item", "load b"), max = 3)

        assertThat(ring.map { it.detail }).containsExactly("onStartCommand action=null", "load a", "load b").inOrder()
        assertThat(ring.first.count).isEqualTo(5)
        assertThat(ring.first.heapMb).isEqualTo(104L)
        assertThat(ring.first.timestampMs).isEqualTo(6L)
    }
}
