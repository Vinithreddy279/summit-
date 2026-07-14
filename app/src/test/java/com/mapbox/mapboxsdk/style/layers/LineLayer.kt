package com.mapbox.mapboxsdk.style.layers

import com.mapbox.mapboxsdk.style.expressions.Expression

class LineLayer(id: String, val sourceId: String) : Layer(id) {
    var sourceLayer: String? = null
    var filter: Expression? = null

    fun withSourceLayer(sourceLayer: String): LineLayer {
        this.sourceLayer = sourceLayer
        return this
    }

    fun withFilter(filter: Expression): LineLayer {
        this.filter = filter
        return this
    }
}
