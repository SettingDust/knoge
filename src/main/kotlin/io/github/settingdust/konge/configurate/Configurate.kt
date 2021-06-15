package io.github.settingdust.konge.configurate

import io.github.settingdust.konge.listen
import io.github.settingdust.konge.properties
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import org.spongepowered.configurate.ScopedConfigurationNode
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.reactive.Subscriber
import org.spongepowered.configurate.reference.ConfigurationReference
import org.spongepowered.configurate.reference.WatchServiceListener
import java.nio.file.Path
import java.nio.file.WatchEvent
import kotlin.io.path.absolute

@OptIn(ExperimentalCoroutinesApi::class)
internal fun ProducerScope<WatchEvent<*>>.produceFlow() = object : Subscriber<WatchEvent<*>> {
    override fun submit(item: WatchEvent<*>) {
        trySendBlocking(item)
    }

    override fun onError(thrown: Throwable) {
        close(thrown)
    }

    override fun onClose() {
        close()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun WatchServiceListener.listenToFile(path: Path): Flow<WatchEvent<*>> = callbackFlow {
    listenToFile(path, produceFlow())
}

@OptIn(ExperimentalCoroutinesApi::class)
fun WatchServiceListener.listenToDirectory(path: Path): Flow<WatchEvent<*>> = callbackFlow {
    listenToDirectory(path, produceFlow())
}

fun <N : ScopedConfigurationNode<N>> Path.listenConfiguration(
    listener: WatchServiceListener,
    loaderFunc: (Path) -> ConfigurationLoader<N>
): ConfigurationReference<N>? = listener.listenToConfiguration(loaderFunc, this)

suspend fun Path.listenProperties(
    listener: WatchServiceListener
): WatchingPropertyResourceBundle {
    val path = absolute()
    val bundle = WatchingPropertyResourceBundle(path)
    listen(listener).collect { bundle.submit(properties()) }
    return bundle
}