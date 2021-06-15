package io.github.settingdust.konge

import io.leangen.geantyref.TypeToken

inline fun <reified T> typeToken(): TypeToken<T> = object: TypeToken<T>() {}