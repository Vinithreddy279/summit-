package com.mapbox.mapboxsdk.style.layers

import com.mapbox.mapboxsdk.style.expressions.Expression

class SymbolLayer(id: String, val sourceId: String) : Layer(id) {
    var sourceLayer: String? = null
    var filter: Expression? = null
}
