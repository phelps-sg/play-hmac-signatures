/*
 * Copyright (c) 2022 Steve Phelps
 */

package com.mesonomics.playhmacsignatures

object Utils {

  /** Conversion from string to Option[Long] for compatibility with Scala 2.12
    */
  implicit class LongStringScala212(number: String) {
    def toLongOpt: Option[Long] = {
      try {
        Some(number.toLong)
      } catch {
        case _: NumberFormatException =>
          None
        case ex: Exception =>
          throw ex
      }
    }

  }

}
