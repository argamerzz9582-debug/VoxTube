package com.voxtube.app.discord

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DiscordFossIsolationTest {
    @Test
    fun `foss classpath excludes functional Discord implementation`() {
        val forbiddenClasses =
            listOf(
                "com.voxtube.app.discord.DiscordTokenStore",
                "com.voxtube.app.discord.DiscordAuthTokens",
                "com.voxtube.app.discord.DiscordPlaybackSource",
                "com.voxtube.app.discord.DiscordPresenceCoordinator",
                "com.voxtube.app.discord.KizzyDiscordPresenceTransport",
                "com.voxtube.app.discord.KizzyGatewayProtocol",
            )

        forbiddenClasses.forEach { className ->
            assertThat(runCatching { Class.forName(className) }.isFailure).isTrue()
        }
    }

    @Test
    fun `foss runtime reports Discord unavailable`() {
        assertThat(DiscordPresenceRuntime.settingsState.value.isAvailable).isFalse()
        assertThat(DiscordPresenceRuntime.settingsState.value.isEnabled).isFalse()
        assertThat(DiscordPresenceRuntime.settingsState.value.summary)
            .isEqualTo(DiscordSettingsSummary.UNAVAILABLE)
    }
}
