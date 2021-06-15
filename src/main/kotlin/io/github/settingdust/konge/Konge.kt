package io.github.settingdust.konge

import com.google.inject.Inject
import org.apache.logging.log4j.Logger
import org.spongepowered.api.Server
import org.spongepowered.api.event.lifecycle.StartingEngineEvent
import org.spongepowered.plugin.PluginContainer
import org.spongepowered.plugin.jvm.Plugin

@Plugin(Konge.PLUGIN_ID)
class Konge @Inject constructor(
    logger: Logger,
    pluginContainer: PluginContainer
) {
    companion object {
        const val PLUGIN_ID = "konge"
    }

    init {
        pluginContainer.registerListener<StartingEngineEvent<Server>> {
            logger.info("Running Kotlin v1.5.0")
        }
    }
}