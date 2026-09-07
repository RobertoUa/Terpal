package io.exoquery.terpal

import kotlin.test.Test

class InterlaceTest: InterpolateTestBase {
  private fun run(parts: List<String>, params: List<String>) =
    ProtoInterpolator.interlace(parts, params, { "" }, { it }, { a, b -> a + b })

  @Test
  fun alternates() {
    run(listOf("a", "b", "c"), listOf("1", "2")) shouldBe "a1b2c"
  }

  @Test
  fun equalSizes() {
    run(listOf("a", "b"), listOf("1", "2")) shouldBe "a1b2"
  }

  // Used to hang: the old loop only advanced while parts had elements left.
  @Test
  fun moreParamsThanParts() {
    run(listOf("a"), listOf("1", "2", "3")) shouldBe "a123"
  }
}
