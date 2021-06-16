/*
 * Configurate
 * Copyright (C) zml and Configurate contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.settingdust.konge.configurate

import io.leangen.geantyref.GenericTypeReflector
import io.leangen.geantyref.TypeToken
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.FieldDiscoverer
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.util.Types
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.AnnotatedType
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

private val dataClassMapperFactory = ObjectMapper.factoryBuilder().addDiscoverer(DataClassFieldDiscoverer).build()

/**
 * Get an object mapper factory with standard capabilities and settings, except
 * for the added ability to interpret data clasess with a [dataClassFieldDiscoverer].
 */
fun objectMapperFactory(): ObjectMapper.Factory {
    return dataClassMapperFactory
}

/**
 * Get a field discoverer that can determine field information from data classes.
 *
 * @sample [org.spongepowered.configurate.kotlin.examples.createLoader]
 */
fun dataClassFieldDiscoverer(): FieldDiscoverer<*> {
    return DataClassFieldDiscoverer
}

/**
 * Get an object mapper for the type [T] using the default object mapper factory
 */
inline fun <reified T> objectMapper(): ObjectMapper<T> {
    return objectMapperFactory()[typeTokenOf()]
}

/**
 * Get an object mapper bound to the instance of [T], resolving type parameters
 */
inline fun <reified T> T.toNode(target: ConfigurationNode) {
    return objectMapperFactory().get<T>().save(this, target)
}

@PublishedApi
internal inline fun <reified T> typeTokenOf() = object : TypeToken<T>() {}

private object DataClassFieldDiscoverer : FieldDiscoverer<MutableMap<KParameter, Any?>> {
    override fun <V> discover(
        target: AnnotatedType,
        collector: FieldDiscoverer.FieldCollector<MutableMap<KParameter, Any?>, V>
    ): FieldDiscoverer.InstanceFactory<MutableMap<KParameter, Any?>>? {
        val klass = GenericTypeReflector.erase(target.type).kotlin
        if (!klass.isData) {
            return null
        }

        val constructor = klass.primaryConstructor ?: return null
        constructor.javaMethod

        val annotatedTypes = constructor.javaConstructor!!.annotatedParameterTypes
        val properties = klass.memberProperties
        constructor.parameters.asSequence().zip(annotatedTypes.asSequence()).forEach { (param, type) ->
            val resolvedType = GenericTypeReflector.resolveType(type, target)
            val field = properties.first { it.name == param.name }

            @Suppress("UNCHECKED_CAST")
            collector.accept(
                param.name,
                resolvedType,
                Types.combinedAnnotations(
                    param.type.javaElement,
                    param.javaElement,
                    field.javaField
                ), // type, backing field, etc
                // deserializer
                { intermediate, arg, implicitProvider ->
                    if (arg != null) {
                        intermediate[param] = arg
                    } else if (!param.isOptional) {
                        intermediate[param] = implicitProvider.get()
                    }
                },
                // serializer
                { (field as KProperty1<V, *>).get(it) }
            )
        }

        return object : FieldDiscoverer.MutableInstanceFactory<MutableMap<KParameter, Any?>> {
            override fun begin(): MutableMap<KParameter, Any?> {
                return mutableMapOf()
            }

            override fun complete(intermediate: MutableMap<KParameter, Any?>): Any {
                return constructor.callBy(intermediate)
            }

            override fun complete(instance: Any?, intermediate: MutableMap<KParameter, Any?>?) {
                if (intermediate == null || instance == null) return
                properties.forEach { prop ->
                    val field = prop.javaField ?: return@forEach
                    val value = intermediate
                        .firstNotNullOfOrNull { if (it.key.name == prop.name) it.value else null }
                        ?: return@forEach
                    field.isAccessible = true
                    field.set(instance, value)
                }
            }

            override fun canCreateInstances(): Boolean = true
        }
    }
}

internal val KAnnotatedElement.javaElement: AnnotatedElement
    get() {
        if (this is KProperty<*>) {
            val javaType = this.javaField ?: this.javaGetter
            if (javaType != null) {
                return javaType
            }
        } else if (this is KFunction<*>) {
            val javaType = this.javaMethod ?: this.javaConstructor
            if (javaType != null) {
                return javaType
            }
        }

        return WrappedElement(this)
    }

internal class WrappedElement(private val backing: KAnnotatedElement) : AnnotatedElement {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Annotation> getAnnotation(annotationClass: Class<T>): T? {
        return backing.annotations.firstOrNull { it.annotationClass.java == annotationClass } as T?
    }

    override fun getAnnotations(): Array<Annotation> {
        return backing.annotations.toTypedArray()
    }

    override fun getDeclaredAnnotations(): Array<Annotation> = this.annotations
}
