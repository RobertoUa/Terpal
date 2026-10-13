package io.exoquery.terpal

import kotlin.test.Test

data class TIn(val value: String)

interface Shape { val tag: String }
interface Rounded: Shape
enum class Circle(override val tag: String): Rounded { SMALL("c") }

open class GenBase<T>(val show: (T) -> String): Interpolator<T, String> {
  override fun interpolate(parts: () -> List<String>, params: () -> List<T>): String =
    interlace(parts(), params().map(show))
}
object GenSub: GenBase<TIn>({ it.value })

abstract class GenWrapBase<T>: InterpolatorWithWrapper<T, String> {
  operator fun invoke(string: String): String = Messages.throwPluginNotExecuted()
  @InterpolatorBackend
  fun interpolate(parts: () -> List<String>, params: () -> List<T>): String =
    interlace(parts(), params().map { show(it) })
  abstract fun show(value: T): String
}

object GenWrapSub: GenWrapBase<TIn>() {
  override fun show(value: TIn) = value.value
  override fun inlined(value: String?) = TIn(value.toString())
  override fun wrap(value: String?) = TIn("s:$value")
  override fun wrap(value: Int?) = TIn("i:$value")
  override fun wrap(value: Long?) = TIn(value.toString())
  override fun wrap(value: Short?) = TIn(value.toString())
  override fun wrap(value: Byte?) = TIn(value.toString())
  override fun wrap(value: Float?) = TIn(value.toString())
  override fun wrap(value: Double?) = TIn(value.toString())
  override fun wrap(value: Boolean?) = TIn(value.toString())
}

object PickTerp: InterpolatorWithWrapper<TIn, String> {
  operator fun invoke(string: String): String = Messages.throwPluginNotExecuted()
  @InterpolatorBackend
  fun interpolate(parts: () -> List<String>, params: () -> List<TIn>): String =
    interlace(parts(), params().map { it.value })

  override fun inlined(value: String?) = TIn(value.toString())
  override fun wrap(value: String?) = TIn("s:$value")
  override fun wrap(value: Int?) = TIn("i:$value")
  override fun wrap(value: Long?) = TIn(value.toString())
  override fun wrap(value: Short?) = TIn(value.toString())
  override fun wrap(value: Byte?) = TIn(value.toString())
  override fun wrap(value: Float?) = TIn(value.toString())
  override fun wrap(value: Double?) = TIn(value.toString())
  override fun wrap(value: Boolean?) = TIn(value.toString())
  fun wrap(value: Shape?) = TIn("shape:${value?.tag}")
  fun wrap(value: Rounded?) = TIn("rounded:${value?.tag}")
}

class InterpolateTypeResolutionTest: InterpolateTestBase {
  @Test
  fun genericBaseInterpolator() {
    GenSub("a ${TIn("x")} b") shouldBe "a x b"
  }

  @Test
  fun genericBaseInterpolatorWithWrapper() {
    val s = "x"
    val i = 1
    GenWrapSub("a ${s} b ${i} c") shouldBe "a s:x b i:1 c"
  }

  fun <X: TIn> spliceTypeParam(x: X) = GenSub("a ${x} b")
  fun <X: Rounded> spliceWrappedTypeParam(x: X) = PickTerp("a ${x} b")

  @Test
  fun typeParameterSplice() {
    spliceTypeParam(TIn("x")) shouldBe "a x b"
    spliceWrappedTypeParam(Circle.SMALL) shouldBe "a rounded:c b"
  }

  @Test
  fun mostSpecificWrapWins() {
    PickTerp("a ${Circle.SMALL} b") shouldBe "a rounded:c b"
  }
}
