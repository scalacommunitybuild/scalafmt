package org.scalafmt

import java.io.File
import scala.meta.Input.stringToInput
import scala.meta.inputs.Input
import scala.util.control.NonFatal
import org.scalafmt.Error.Incomplete
import org.scalafmt.config.FormatEvent.CreateFormatOps
import org.scalafmt.config.LineEndings.preserve
import org.scalafmt.config.LineEndings.windows
import org.scalafmt.config.ScalafmtConfig
import org.scalafmt.internal.BestFirstSearch
import org.scalafmt.internal.FormatOps
import org.scalafmt.internal.FormatWriter
import org.scalafmt.rewrite.Rewrite
import org.scalafmt.util.InputOps

object Scalafmt {

  private val WindowsLineEnding = "\r\n"
  private val UnixLineEnding = "\n"

  /** Format a string. See [[org.scalafmt.Scalafmt.formatInput]]. */
  def format(code: String,
             style: ScalafmtConfig = ScalafmtConfig.default,
             range: Set[Range] = Set.empty[Range]): Formatted = {
    formatInput(Input.String(code), style, range)
  }

  /** Format a file. See [[org.scalafmt.Scalafmt.formatInput]]. */
  def formatFile(file: File,
                 style: ScalafmtConfig = ScalafmtConfig.default,
                 range: Set[Range] = Set.empty[Range]): Formatted = {
    formatInput(Input.File(file), style, range)
  }

  /**
    * Format Scala code using scalafmt.
    *
    * @param input Code string to format. Common values
    *              - [[scala.meta.Input.String]] for unlabeled strings.
    *              - [[scala.meta.Input.File]] for a file on the local filesystem.
    *              - [[scala.meta.Input.LabeledString]] for an in-memory string with label.
    *
    *              When a label is provided, the reported error messages use the following format
    *
    *                Foo.scala:2 error: expected ; but end of file found.
    *
    * @param style Configuration for formatting output.
    * @param range EXPERIMENTAL. Format a subset of lines.
    * @return [[Formatted.Success]] if successful,
    *         [[Formatted.Failure]] otherwise. If you are OK with throwing
    *         exceptions, use [[Formatted.Success.get]] to get back a
    *         string.
    */
  def formatInput(input: Input,
                  style: ScalafmtConfig = ScalafmtConfig.default,
                  range: Set[Range] = Set.empty[Range]): Formatted = {
    try {
      val code = new String(input.chars)
      val runner = style.runner
      if (code.matches("\\s*")) Formatted.Success(System.lineSeparator())
      else {
        val isWindows = containsWindowsLineEndings(code)
        val unixCode: Input = if (isWindows) {
          InputOps.map(input)(
            _.replaceAllLiterally(WindowsLineEnding, UnixLineEnding))
        } else {
          input
        }
        val toParse = Rewrite(unixCode, style)
        val tree = runner.dialect(toParse).parse(runner.parser).get
        val formatOps = new FormatOps(tree, style)
        runner.eventCallback(CreateFormatOps(formatOps))
        val formatWriter = new FormatWriter(formatOps)
        val search = new BestFirstSearch(formatOps, range, formatWriter)
        val partial = search.getBestPath
        val formattedString = formatWriter.mkString(partial.splits)
        val correctedFormattedString =
          if ((style.lineEndings == preserve && isWindows) ||
              style.lineEndings == windows) {
            formattedString.replaceAllLiterally(UnixLineEnding,
                                                WindowsLineEnding)
          } else {
            formattedString
          }
        if (partial.reachedEOF) {
          Formatted.Success(correctedFormattedString)
        } else {
          throw Incomplete(correctedFormattedString)
        }
      }
    } catch {
      // TODO(olafur) add more fine grained errors.
      case NonFatal(e) => Formatted.Failure(e)
    }
  }

  private[this] def containsWindowsLineEndings(code: String): Boolean =
    code.contains(WindowsLineEnding)
}
