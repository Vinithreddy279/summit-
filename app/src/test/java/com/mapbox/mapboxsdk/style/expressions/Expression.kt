package com.mapbox.mapboxsdk.style.expressions

open class Expression {
    companion object {
        @JvmStatic fun all(vararg expressions: Expression) = Expression()
        @JvmStatic fun any(vararg expressions: Expression) = Expression()
        @JvmStatic fun eq(left: Expression, right: Expression) = Expression()
        @JvmStatic fun eq(left: Expression, right: String) = Expression()
        @JvmStatic fun has(property: String) = Expression()
        @JvmStatic fun get(property: String) = Expression()
        @JvmStatic fun literal(value: String) = Expression()
        @JvmStatic fun literal(value: Number) = Expression()
        @JvmStatic fun literal(value: Boolean) = Expression()
        @JvmStatic fun literal(value: Any) = Expression()
        @JvmStatic fun switchCase(vararg expressions: Expression) = Expression()
        @JvmStatic fun interpolate(interpolator: Interpolator, number: Expression, vararg stops: Stop) = Expression()
        @JvmStatic fun linear(): Interpolator = Interpolator()
        @JvmStatic fun zoom() = Expression()
        @JvmStatic fun stop(value: Any, output: Any) = Stop(value, output)
    }

    class Stop(val value: Any, val output: Any)
    open class Interpolator : Expression()
}
