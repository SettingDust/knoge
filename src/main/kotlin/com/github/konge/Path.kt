package com.github.konge

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.runBlocking
import org.spongepowered.configurate.reference.WatchServiceListener
import java.nio.file.Path
import java.nio.file.WatchEvent
import java.util.*
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory
import kotlin.io.path.reader

@OptIn(ExperimentalCoroutinesApi::class)
fun Path.listen(
    listener: WatchServiceListener
): Flow<WatchEvent<*>> {
    val path = absolute()
    return callbackFlow {
        val callback: (WatchEvent<*>) -> Unit = {
            trySendBlocking(it)
        }
        if (isDirectory()) {
            listener.listenToDirectory(path, callback)
        } else {
            listener.listenToFile(path, callback)
        }
    }
}

fun Path.properties() = PropertyResourceBundle(reader())