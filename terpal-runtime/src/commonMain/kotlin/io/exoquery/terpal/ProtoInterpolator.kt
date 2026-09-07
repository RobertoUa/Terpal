package io.exoquery.terpal

interface InterpolatorWithWrapper<T, R>: ProtoInterpolator<T, R> {
  fun inlined(value: String?): T
  fun wrap(value: String?): T
  fun wrap(value: Int?): T
  fun wrap(value: Long?): T
  fun wrap(value: Short?): T
  fun wrap(value: Byte?): T
  fun wrap(value: Float?): T
  fun wrap(value: Double?): T
  fun wrap(value: Boolean?): T
}

// TODO also detect static constants in SQL(...) and warn about them

@PotentiallyDangerous
fun inline(value: String): String =
  throw IllegalStateException("This function can only be used inside of a Terpal interpolator invocation.")

interface Interpolator<T, R>: ProtoInterpolator<T, R> {
  operator fun invoke(string: String): R = Messages.throwPluginNotExecuted()
  @InterpolatorBackend
  fun interpolate(parts: () -> List<String>, params: () -> List<T>): R
}

interface ProtoInterpolator<T, R> {
  companion object {
    fun <T> interlace(parts: List<String>, params: List<T>, empty: () -> T, lift: (String) -> T, combine: (T, T) -> T): T {
      // Index-driven because the previous iterator form spun forever once parts
      // ran out while params had not.
      var curr = empty()
      val n = parts.size
      val m = params.size
      var i = 0
      while (i < n) {
        curr = combine(curr, lift(parts[i]))
        if (i < m) curr = combine(curr, params[i])
        i++
      }
      // Surplus params still get folded in rather than dropped or hung on.
      while (i < m) {
        curr = combine(curr, params[i])
        i++
      }
      return curr
    }
  }
}
