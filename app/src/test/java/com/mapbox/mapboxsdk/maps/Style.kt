package com.mapbox.mapboxsdk.maps

import com.mapbox.mapboxsdk.style.layers.Layer

class Style {
    val layers = mutableListOf<Layer>()
    val removedLayers = mutableListOf<Layer>()
    val addedLayers = mutableListOf<Layer>()
    val addedLayersBelow = mutableListOf<Pair<Layer, String>>()

    fun getLayer(id: String): Layer? {
        return layers.find { it.id == id }
    }

    fun removeLayer(layer: Layer): Boolean {
        val removed = layers.remove(layer)
        removedLayers.add(layer)
        return removed
    }

    fun addLayer(layer: Layer) {
        layers.add(layer)
        addedLayers.add(layer)
    }

    fun addLayerBelow(layer: Layer, belowId: String) {
        layers.add(layer)
        addedLayersBelow.add(Pair(layer, belowId))
    }
}
