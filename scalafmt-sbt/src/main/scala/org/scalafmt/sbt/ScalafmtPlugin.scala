package org.scalafmt.sbt
import sbt._, Keys._
import org.scalafmt.cli.Cli

object ScalafmtPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  object autoImport {
    val scalafmt: Command =
      Command.args("scalafmt", "run the scalafmt command line interface.") {
        case (state, args) =>
          val exit = Cli.mainExitCode("--non-interactive" +: args.toArray)
          if (exit != 0) {
            sys.error(s"Non-zero exit code=$exit running scalafmt.")
          }
          state
      }
  }
  override def globalSettings: Seq[Def.Setting[_]] =
    Seq(
      commands += autoImport.scalafmt
    ) ++
      addCommandAlias("scalafmtTest", "scalafmt --test") ++
      addCommandAlias("scalafmtDiffTest", "scalafmt --diff --test") ++
      addCommandAlias("scalafmtDiff", "scalafmt --diff")

}
