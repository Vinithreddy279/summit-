package com.mapbox.mapboxsdk.style.layers

open class Layer(val id: String) {
    val properties = mutableMapOf<String, Any>()

    fun setProperties(vararg params: PropertyValue<*>) {
        params.forEach {
            properties[it.name] = it.value ?: ""
        }
    }
}
