package com.github.konge

import io.leangen.geantyref.TypeToken

inline fun <reified T> typeToken(): TypeToken<T> = object: TypeToken<T>() {}