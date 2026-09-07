package io.exoquery.terpal

import kotlin.test.assertEquals

interface InterpolateTestBase {
  infix fun <T> T.shouldBe(other: T) = assertEquals(other, this)
}

// Need to have either a string or empty-space both before all the characters and after
// That means that if there's nothing after the last param we need to add an empty space
// character (which in this case we make into a "-") in order to easily understand what's happening.

fun interlace(parts: List<String>, params: List<String>): String {
  val out = StringBuilder()
  var i = 0
  while (i < parts.size) {
    out.append(parts[i])
    if (i < params.size) out.append(params[i])
    i++
  }
  while (i < params.size) {
    out.append(params[i])
    i++
  }
  return out.toString()
}