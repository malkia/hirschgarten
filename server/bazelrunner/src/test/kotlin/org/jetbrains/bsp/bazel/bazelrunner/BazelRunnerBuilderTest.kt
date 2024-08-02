package org.jetbrains.bsp.bazel.bazelrunner

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.jetbrains.bsp.bazel.workspacecontext.BazelBinarySpec
import org.jetbrains.bsp.bazel.workspacecontext.BuildFlagsSpec
import org.jetbrains.bsp.bazel.workspacecontext.BuildManualTargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.DirectoriesSpec
import org.jetbrains.bsp.bazel.workspacecontext.DotBazelBspDirPathSpec
import org.jetbrains.bsp.bazel.workspacecontext.EnabledRulesSpec
import org.jetbrains.bsp.bazel.workspacecontext.ExperimentalAddTransitiveCompileTimeJars
import org.jetbrains.bsp.bazel.workspacecontext.ExperimentalUseLibOverModSpec
import org.jetbrains.bsp.bazel.workspacecontext.IdeJavaHomeOverrideSpec
import org.jetbrains.bsp.bazel.workspacecontext.ImportDepthSpec
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.junit.jupiter.api.Test
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag
import kotlin.io.path.Path

fun String.bsp() = BuildTargetIdentifier(this)

val mockContext = WorkspaceContext(
  targets = TargetsSpec(listOf("in1".bsp(), "in2".bsp()), listOf("ex1".bsp(), "ex2".bsp())),
  directories = DirectoriesSpec(listOf(Path("in1dir"), Path("in2dir")), listOf(Path("ex1dir"), Path("ex2dir"))),
  buildFlags = BuildFlagsSpec(listOf("flag1", "flag2")),
  bazelBinary = BazelBinarySpec(Path("bazel")),
  buildManualTargets = BuildManualTargetsSpec(true),
  dotBazelBspDirPath = DotBazelBspDirPathSpec(Path(".bazelbsp")),
  importDepth = ImportDepthSpec(2),
  enabledRules = EnabledRulesSpec(listOf("rule1", "rule2")),
  ideJavaHomeOverrideSpec = IdeJavaHomeOverrideSpec(Path("java_home")),
  experimentalUseLibOverModSection = ExperimentalUseLibOverModSpec(true),
  experimentalAddTransitiveCompileTimeJars = ExperimentalAddTransitiveCompileTimeJars(true),
)

val contextProvider = object : WorkspaceContextProvider {
  override fun currentWorkspaceContext(): WorkspaceContext = mockContext
}

val bazelRunner = BazelRunner(contextProvider, null, Path("workspaceRoot"))

class BazelRunnerBuilderTest {
  @Test
  fun `most bare bones build without targets (even though it's not correct)`() {
    val builder = bazelRunner.commandBuilder()

    builder.build().withUseBuildFlags(false).dump() shouldContainExactly listOf(
      "bazel",
      "build",
      "--override_repository=bazelbsp_aspect=.bazelbsp",
      BazelFlag.toolTag()
    )
  }

  @Test
  fun `build with targets`() {
    val builder = bazelRunner.commandBuilder()

    builder.build().withTargets(mockContext.targets).dump() shouldContainExactly listOf(
      "bazel",
      "build",
      "flag1", "flag2",
      "--override_repository=bazelbsp_aspect=.bazelbsp",
      BazelFlag.toolTag(),
       "--", "in1", "in2", "-ex1", "-ex2"

    )
  }


}

