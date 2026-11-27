package com.voxtube.app.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VoxTubeRequestLogTest {
    @Test
    fun `only YouTube API hosts are logged`() {
        assertThat(VoxTubeRequestLog.isYouTubeHost("www.youtube.com")).isTrue()
        assertThat(VoxTubeRequestLog.isYouTubeHost("music.youtube.com")).isTrue()
        assertThat(VoxTubeRequestLog.isYouTubeHost("youtubei.googleapis.com")).isTrue()
        assertThat(VoxTubeRequestLog.isYouTubeHost("rr3---sn-abc.googlevideo.com")).isFalse()
        assertThat(VoxTubeRequestLog.isYouTubeHost("notyoutube.com")).isFalse()
    }

    @Test
    fun `video id and client name come from the request body`() {
        val line =
            VoxTubeRequestLog.describe(
                method = "POST",
                host = "www.youtube.com",
                path = "/youtubei/v1/next",
                clientHeader = "1",
                body = """{"context":{"client":{"clientName":"WEB"}},"videoId":"abcdefghijk"}""",
                videoQuery = null,
            )

        assertThat(line).isEqualTo("POST /youtubei/v1/next WEB www.youtube.com v=abcdefghijk")
    }

    @Test
    fun `continuation requests log a short token prefix`() {
        val line =
            VoxTubeRequestLog.describe(
                method = "POST",
                host = "www.youtube.com",
                path = "/youtubei/v1/browse",
                clientHeader = "1",
                body = """{"continuation":"Eg0SC2FiY2RlZmdoaWprGAYyJSIRIgthYmNkZWZnaGlqaw"}""",
                videoQuery = null,
            )

        assertThat(line).isEqualTo("POST /youtubei/v1/browse 1 www.youtube.com c=Eg0SC2FiY2RlZmdo")
    }

    @Test
    fun `a bodiless request falls back to the query video id`() {
        val line =
            VoxTubeRequestLog.describe(
                method = "GET",
                host = "www.youtube.com",
                path = "/watch",
                clientHeader = null,
                body = null,
                videoQuery = "abcdefghijk",
            )

        assertThat(line).isEqualTo("GET /watch - www.youtube.com v=abcdefghijk")
    }
}
