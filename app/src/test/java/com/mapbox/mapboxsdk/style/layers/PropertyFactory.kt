package com.mapbox.mapboxsdk.style.layers

import com.mapbox.mapboxsdk.style.expressions.Expression

object PropertyFactory {
    @JvmStatic fun lineColor(value: String) = PropertyValue("line-color", value)
    @JvmStatic fun lineWidth(value: Float?) = PropertyValue("line-width", value)
    @JvmStatic fun lineOpacity(value: Float?) = PropertyValue("line-opacity", value)
    @JvmStatic fun iconSize(value: Float?) = PropertyValue("icon-size", value)
    @JvmStatic fun iconSize(value: Expression) = PropertyValue("icon-size", value)
    @JvmStatic fun iconSize(value: Any) = PropertyValue("icon-size", value)
}
