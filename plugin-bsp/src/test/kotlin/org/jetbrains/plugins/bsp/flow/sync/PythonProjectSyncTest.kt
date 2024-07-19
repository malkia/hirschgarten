package org.jetbrains.plugins.bsp.flow.sync

import ch.epfl.scala.bsp4j.CleanCacheParams
import ch.epfl.scala.bsp4j.CleanCacheResult
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.CppOptionsParams
import ch.epfl.scala.bsp4j.CppOptionsResult
import ch.epfl.scala.bsp4j.DebugSessionAddress
import ch.epfl.scala.bsp4j.DebugSessionParams
import ch.epfl.scala.bsp4j.DependencyModulesParams
import ch.epfl.scala.bsp4j.DependencyModulesResult
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.InitializeBuildResult
import ch.epfl.scala.bsp4j.InverseSourcesParams
import ch.epfl.scala.bsp4j.InverseSourcesResult
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.JavacOptionsResult
import ch.epfl.scala.bsp4j.JvmCompileClasspathParams
import ch.epfl.scala.bsp4j.JvmCompileClasspathResult
import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams
import ch.epfl.scala.bsp4j.JvmRunEnvironmentResult
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult
import ch.epfl.scala.bsp4j.OutputPathsParams
import ch.epfl.scala.bsp4j.OutputPathsResult
import ch.epfl.scala.bsp4j.PythonOptionsParams
import ch.epfl.scala.bsp4j.PythonOptionsResult
import ch.epfl.scala.bsp4j.ReadParams
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.RunParams
import ch.epfl.scala.bsp4j.RunResult
import ch.epfl.scala.bsp4j.RustWorkspaceParams
import ch.epfl.scala.bsp4j.RustWorkspaceResult
import ch.epfl.scala.bsp4j.ScalaMainClassesParams
import ch.epfl.scala.bsp4j.ScalaMainClassesResult
import ch.epfl.scala.bsp4j.ScalaTestClassesParams
import ch.epfl.scala.bsp4j.ScalaTestClassesResult
import ch.epfl.scala.bsp4j.ScalacOptionsParams
import ch.epfl.scala.bsp4j.ScalacOptionsResult
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.TestParams
import ch.epfl.scala.bsp4j.TestResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.MobileInstallParams
import org.jetbrains.bsp.protocol.MobileInstallResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresProvider
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.WorkspaceModelProjectStructureDiff
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class PythonJoinedBuildServer(private val pythonOptionsResult: PythonOptionsResult) : JoinedBuildServer {
  override fun buildInitialize(p0: InitializeBuildParams?): CompletableFuture<InitializeBuildResult> {
    TODO("Not yet implemented")
  }

  override fun onBuildInitialized() {
    TODO("Not yet implemented")
  }

  override fun buildShutdown(): CompletableFuture<Any> {
    TODO("Not yet implemented")
  }

  override fun onBuildExit() {
    TODO("Not yet implemented")
  }

  override fun workspaceBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult> {
    TODO("Not yet implemented")
  }

  override fun workspaceReload(): CompletableFuture<Any> {
    TODO("Not yet implemented")
  }

  override fun buildTargetSources(p0: SourcesParams?): CompletableFuture<SourcesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetInverseSources(p0: InverseSourcesParams?): CompletableFuture<InverseSourcesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetDependencySources(p0: DependencySourcesParams?): CompletableFuture<DependencySourcesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetDependencyModules(p0: DependencyModulesParams?): CompletableFuture<DependencyModulesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetResources(p0: ResourcesParams?): CompletableFuture<ResourcesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetOutputPaths(p0: OutputPathsParams?): CompletableFuture<OutputPathsResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetCompile(p0: CompileParams?): CompletableFuture<CompileResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetRun(p0: RunParams?): CompletableFuture<RunResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetTest(p0: TestParams?): CompletableFuture<TestResult> {
    TODO("Not yet implemented")
  }

  override fun debugSessionStart(p0: DebugSessionParams?): CompletableFuture<DebugSessionAddress> {
    TODO("Not yet implemented")
  }

  override fun buildTargetCleanCache(p0: CleanCacheParams?): CompletableFuture<CleanCacheResult> {
    TODO("Not yet implemented")
  }

  override fun onRunReadStdin(p0: ReadParams?) {
    TODO("Not yet implemented")
  }

  override fun buildTargetJvmTestEnvironment(p0: JvmTestEnvironmentParams?): CompletableFuture<JvmTestEnvironmentResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetJvmRunEnvironment(p0: JvmRunEnvironmentParams?): CompletableFuture<JvmRunEnvironmentResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetJvmCompileClasspath(p0: JvmCompileClasspathParams?): CompletableFuture<JvmCompileClasspathResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetScalacOptions(p0: ScalacOptionsParams?): CompletableFuture<ScalacOptionsResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetScalaTestClasses(p0: ScalaTestClassesParams?): CompletableFuture<ScalaTestClassesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetScalaMainClasses(p0: ScalaMainClassesParams?): CompletableFuture<ScalaMainClassesResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetJavacOptions(p0: JavacOptionsParams?): CompletableFuture<JavacOptionsResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetCppOptions(p0: CppOptionsParams?): CompletableFuture<CppOptionsResult> {
    TODO("Not yet implemented")
  }

  override fun workspaceLibraries(): CompletableFuture<WorkspaceLibrariesResult> {
    TODO("Not yet implemented")
  }

  override fun workspaceDirectories(): CompletableFuture<WorkspaceDirectoriesResult> {
    TODO("Not yet implemented")
  }

  override fun workspaceInvalidTargets(): CompletableFuture<WorkspaceInvalidTargetsResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetRunWithDebug(params: RunWithDebugParams): CompletableFuture<RunResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetMobileInstall(params: MobileInstallParams): CompletableFuture<MobileInstallResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): CompletableFuture<JvmBinaryJarsResult> {
    TODO("Not yet implemented")
  }

  override fun workspaceBuildAndGetBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult> {
    TODO("Not yet implemented")
  }

  override fun buildTargetPythonOptions(p0: PythonOptionsParams?): CompletableFuture<PythonOptionsResult> =
    CompletableFuture.completedFuture(pythonOptionsResult)

  override fun rustWorkspace(p0: RustWorkspaceParams?): CompletableFuture<RustWorkspaceResult> {
    TODO("Not yet implemented")
  }

}

class PythonProjectSyncTest : MockProjectBaseTest() {
  lateinit var hook: ProjectSyncHook

  @BeforeEach
  override fun beforeEach() {
    // given
    hook = PythonProjectSync()
  }

  @Test
  fun test() {
    // given
    val server = PythonJoinedBuildServer(PythonOptionsResult(emptyList()))
    val capabilities = BazelBuildServerCapabilities()
    val diff = AllProjectStructuresProvider(project).newDiff()

    val baseTargetInfos = BaseTargetInfos(
      allTargetIds = emptyList(), // TODO
      infos = emptyList(), // TODO
    )

    // when
    runBlocking {
      reportSequentialProgress { reporter ->
        hook.onSync(project, server, capabilities, diff, "test", reporter, baseTargetInfos, CompletableFuture<Void>().newIncompleteFuture(),{})
      }
    }
    // then

    diff.workspaceModelDiff.mutableEntityStorage.entities(ModuleEntity::class.java).toList() shouldBe emptyList() // TODO
  }
}