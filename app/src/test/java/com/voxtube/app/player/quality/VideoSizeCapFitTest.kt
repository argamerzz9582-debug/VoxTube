package com.voxtube.app.player.quality

import com.google.common.truth.Truth.assertThat
import com.voxtube.app.player.config.VideoSizeCap
import com.voxtube.app.player.quality.LadderTestStreams.h264
import org.junit.Test

class VideoSizeCapFitTest {
    private val fullHd = VideoSizeCap(1920, 1080)

    @Test
    fun `streams fit the cap in either orientation`() {
        assertThat(fullHd.fits(h264(1080, 4_500_000))).isTrue()
        assertThat(VideoSizeCap(1080, 1920).fits(h264(1080, 4_500_000))).isTrue()
    }

    @Test
    fun `a stream over the cap does not fit`() {
        assertThat(VideoSizeCap(1280, 720).fits(h264(1080, 4_500_000))).isFalse()
    }
}
