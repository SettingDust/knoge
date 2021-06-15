package io.github.settingdust.konge

import io.leangen.geantyref.TypeToken
import org.spongepowered.api.Sponge
import org.spongepowered.api.event.Event
import org.spongepowered.api.event.Order
import org.spongepowered.plugin.PluginContainer

fun <T : Event> PluginContainer.registerListener(
    eventType: TypeToken<T>,
    order: Order = Order.DEFAULT,
    beforeModifications: Boolean = false,
    listener: T.() -> Unit
) = Sponge.eventManager().registerListener(this, eventType, order, beforeModifications, listener)

inline fun <reified T : Event> PluginContainer.registerListener(
    order: Order = Order.DEFAULT,
    beforeModifications: Boolean = false,
    noinline listener: T.() -> Unit
) = registerListener(typeToken(), order, beforeModifications, listener)

fun PluginContainer.registerListeners(receiver: Any) = Sponge.eventManager().registerListeners(this, receiver)
