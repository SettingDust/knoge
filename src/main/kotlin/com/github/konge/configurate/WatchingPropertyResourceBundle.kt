package com.github.konge.configurate

import org.spongepowered.configurate.reactive.Subscriber
import java.nio.file.Path
import java.util.*
import kotlin.io.path.inputStream

class WatchingPropertyResourceBundle constructor(
    path: Path
) : ResourceBundle(), Subscriber<PropertyResourceBundle> {

    private var propertyResourceBundle: PropertyResourceBundle

    override fun submit(item: PropertyResourceBundle) {
        propertyResourceBundle = item
    }

    public override fun setParent(parent: ResourceBundle?) = super.setParent(parent)

    public override fun handleGetObject(key: String): Any? = propertyResourceBundle.handleGetObject(key)

    override fun getKeys(): Enumeration<String> = propertyResourceBundle.keys

    override fun handleKeySet(): Set<String> = propertyResourceBundle.keySet()

    init {
        propertyResourceBundle = PropertyResourceBundle(path.inputStream())
    }
}