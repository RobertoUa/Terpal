package io.exoquery.terpal

import kotlin.test.Test

class InterpolateReceiverTest: InterpolateTestBase {
  data class RIn(val value: String)

  class WrapTerp: InterpolatorWithWrapper<RIn, String> {
    operator fun invoke(string: String): String = Messages.throwPluginNotExecuted()
    @InterpolatorBackend
    fun interpolate(parts: () -> List<String>, params: () -> List<RIn>): String =
      interlace(parts(), params().map { it.value })

    override fun inlined(value: String?) = RIn(value.toString())
    override fun wrap(value: String?) = RIn("s:$value")
    override fun wrap(value: Int?) = RIn("i:$value")
    override fun wrap(value: Long?) = RIn(value.toString())
    override fun wrap(value: Short?) = RIn(value.toString())
    override fun wrap(value: Byte?) = RIn(value.toString())
    override fun wrap(value: Float?) = RIn(value.toString())
    override fun wrap(value: Double?) = RIn(value.toString())
    override fun wrap(value: Boolean?) = RIn(value.toString())
  }

  class BatchTerp: InterpolatorBatchingWithWrapper<RIn> {
    override fun <T: Any> invoke(create: (T) -> String): Pair<List<String>, (T) -> List<RIn>> = Messages.throwPluginNotExecuted()
    override fun <T: Any> interpolate(parts: () -> List<String>, params: (T) -> List<RIn>): Pair<List<String>, (T) -> List<RIn>> =
      parts() to params

    override fun inlined(value: String?) = RIn(value.toString())
    override fun wrap(value: String?) = RIn("s:$value")
    override fun wrap(value: Int?) = RIn("i:$value")
    override fun wrap(value: Long?) = RIn(value.toString())
    override fun wrap(value: Short?) = RIn(value.toString())
    override fun wrap(value: Byte?) = RIn(value.toString())
    override fun wrap(value: Float?) = RIn(value.toString())
    override fun wrap(value: Double?) = RIn(value.toString())
    override fun wrap(value: Boolean?) = RIn(value.toString())
  }

  var evals = 0
  fun terp(): WrapTerp { evals++; return WrapTerp() }
  fun batchTerp(): BatchTerp { evals++; return BatchTerp() }

  val s = "x"
  val i = 1
  val fromInitializer = terp()("a ${s} b ${i} c")

  @Test
  fun receiverEvaluatedOnce() {
    evals = 0
    terp()("a ${s} b ${i} c") shouldBe "a s:x b i:1 c"
    evals shouldBe 1
  }

  @Test
  fun receiverInPropertyInitializer() {
    fromInitializer shouldBe "a s:x b i:1 c"
  }

  @Test
  fun batchingReceiverEvaluatedOnce() {
    evals = 0
    val (parts, params) = batchTerp()({ n: Int -> "a ${n} b ${s} c" })
    parts shouldBe listOf("a ", " b ", " c")
    params(7) shouldBe listOf(RIn("i:7"), RIn("s:x"))
    evals shouldBe 1
  }
}
