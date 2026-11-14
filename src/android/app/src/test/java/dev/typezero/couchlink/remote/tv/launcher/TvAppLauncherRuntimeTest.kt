package dev.typezero.couchlink.remote.tv.launcher

import dev.typezero.couchlink.remote.tv.provider.*
import dev.typezero.couchlink.remote.tv.TvRemoteClient
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TvAppLauncherRuntimeTest {
    @Test fun disconnectedRequestReportsWhyItWasNotSubmitted() {
        val providerRuntime = TvProviderRuntime(RuntimeEnvironment.getApplication())
        try {
            val runtime = TvAppLauncherRuntime(RuntimeEnvironment.getApplication(), providerRuntime)
            val shortcut = TvAppShortcut("saved", "Netflix", TvProviderId.GoogleAndroidTv, "https://www.netflix.com/browse", TvAppShortcutKind.KnownService, "netflix")
            assertEquals("Disconnected", runtime.launch(shortcut).toString())
        } finally { providerRuntime.close() }
    }
    @Test fun providerAndCapabilityGuardsNeverDispatchRequests() {
        val provider = RecordingTvProvider()
        val runtime = TvAppLauncherRuntime(TvAppShortcutStore(RuntimeEnvironment.getApplication())) { provider }
        val shortcut = TvAppShortcut("saved", "Hulu", TvProviderId.GoogleAndroidTv, "hulu://video/", TvAppShortcutKind.KnownService, "hulu")
        assertEquals(TvAppLaunchResult.WrongProvider, runtime.launch(shortcut.copy(providerId = TvProviderId.Roku)))
        provider.state.value = provider.state.value.copy(capabilities = emptySet())
        assertEquals(TvAppLaunchResult.Unsupported, runtime.launch(shortcut))
        provider.state.value = provider.state.value.copy(capabilities = setOf(TvCapability.AppLaunch), connection = TvProviderConnectionState())
        assertEquals(TvAppLaunchResult.Disconnected, runtime.launch(shortcut))
        assertTrue(provider.requests.isEmpty())
    }
    @Test fun requestUsesCurrentProviderAndPreservesExactTarget() {
        val first = RecordingTvProvider()
        val second = RecordingTvProvider()
        var active = first
        val runtime = TvAppLauncherRuntime(TvAppShortcutStore(RuntimeEnvironment.getApplication())) { active }
        val shortcut = TvAppShortcut("saved", "Hulu", TvProviderId.GoogleAndroidTv, "hulu://video/", TvAppShortcutKind.KnownService, "hulu")
        active = second
        assertEquals(TvAppLaunchResult.Requested, runtime.launch(shortcut))
        assertTrue(first.requests.isEmpty())
        assertEquals(listOf(shortcut.toProviderApp()), second.requests)
        second.failure = IllegalStateException("Closed transport")
        assertEquals(TvAppLaunchResult.Failed, runtime.launch(shortcut))
        assertEquals(1, second.requests.size)
    }
    @Test fun runtimeAddsKnownPresetsWithoutUserEnteredUrisOrDuplicates() {
        val store = TvAppShortcutStore(RuntimeEnvironment.getApplication())
        store.clear()
        val runtime = TvAppLauncherRuntime(store) { RecordingTvProvider() }
        val one = runtime.addKnownService("apple-tv", TvProviderId.GoogleAndroidTv)
        assertEquals("https://tv.apple.com/", one.launchTarget)
        assertEquals("apple-tv", one.knownServiceId)
        assertEquals(one.id, runtime.addKnownService("apple-tv", TvProviderId.GoogleAndroidTv).id)
        assertEquals(1, store.shortcuts.value.size)
        assertThrows(IllegalArgumentException::class.java) { runtime.addKnownService("prime-video", TvProviderId.GoogleAndroidTv) }
        assertThrows(IllegalArgumentException::class.java) { runtime.addKnownService("hulu", TvProviderId.Roku) }
        assertThrows(IllegalArgumentException::class.java) { runtime.addCustomApp("Bad", TvProviderId.GoogleAndroidTv, "not a uri") }
    }
    @Test fun failedWireWriteReturnsFalseAndMarksConnectionUnavailable() {
        val client = TvRemoteClient(RuntimeEnvironment.getApplication())
        val output = object : java.io.OutputStream() { override fun write(value: Int) { throw java.io.IOException("Closed stream") } }
        TvRemoteClient::class.java.getDeclaredField("output").apply { isAccessible = true }.set(client, BufferedOutputStream(output))
        assertFalse(client.launchAppLink("hulu://video/"))
        val state = TvRemoteClient::class.java.getDeclaredField("currentConnection").apply { isAccessible = true }.get(client) as TvRemoteClient.Connection
        assertFalse(state.ready)
        assertFalse(state.connected)
        assertTrue(state.message.startsWith("TV command failed:"))
        assertEquals(0L, state.commandsSent)
    }
    @Test fun aSuccessfulWireWriteReportsOnlyRequestSentNotAppOpened() {
        val client = TvRemoteClient(RuntimeEnvironment.getApplication())
        val bytes = ByteArrayOutputStream()
        TvRemoteClient::class.java.getDeclaredField("output").apply { isAccessible = true }.set(client, BufferedOutputStream(bytes))
        assertTrue(client.launchAppLink("hulu://video/"))
        val state = TvRemoteClient::class.java.getDeclaredField("currentConnection").apply { isAccessible = true }.get(client) as TvRemoteClient.Connection
        assertEquals("Launch request sent to TV.", state.message)
        assertEquals("APP_LINK", state.lastCommand)
        assertTrue(bytes.size() > 0)
    }
}
