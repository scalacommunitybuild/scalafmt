package org.scalafmt.util

import scala.meta.inputs.Input

object InputOps {
  def map(input: Input)(f: String => String): Input = {
    val str = new String(input.chars)
    val newStr = f(str)
    input match {
      case Input.File(path, _)          => Input.LabeledString(path.toString(), newStr)
      case Input.LabeledString(path, _) => Input.LabeledString(path, newStr)
      case _                            => Input.String(newStr)
    }
  }
}
