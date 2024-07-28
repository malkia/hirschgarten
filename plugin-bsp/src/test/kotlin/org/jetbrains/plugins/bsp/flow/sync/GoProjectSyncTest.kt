package org.jetbrains.plugins.bsp.flow.sync

import ch.epfl.scala.bsp4j.*
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
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class GoJoinedBuildServer(private val pythonOptionsResult: PythonOptionsResult) : JoinedBuildServer {
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

class GoProjectSyncTest : MockProjectBaseTest() {
  lateinit var hook: ProjectSyncHook

  @BeforeEach
  override fun beforeEach() {
    // given
    hook = GoProjectSync()
  }

  @Test
  fun test() {
    // given
    val server = GoJoinedBuildServer(PythonOptionsResult(emptyList()))
    val capabilities = BazelBuildServerCapabilities()
    val diff = AllProjectStructuresProvider(project).newDiff()

    val goLibrary1 = generateTarget(
      targetId = BuildTargetIdentifier("@@server/lib:hello_lib"),
      type = "library",
      sourcesItems = listOf("file:///root/server/lib/file1.go"),
    )
    val goLibrary2 = generateTarget(
      targetId = BuildTargetIdentifier("@server/parser:parser_lib"),
      dependencies = listOf(goLibrary1.target.id),
      type = "library",
      sourcesItems = listOf("file:///root/server/lib/file1.go"),
    )
    val goApplication = generateTarget(
      targetId = BuildTargetIdentifier("@server:main_app"),
      type = "application",
      dependencies = listOf(goLibrary1.target.id, goLibrary2.target.id),
      sourcesItems = listOf("file:///root/server/main_file.go"),
    )
    val targets = listOf(goLibrary1, goLibrary2, goApplication)

    val baseTargetInfos = BaseTargetInfos(
      allTargetIds = targets.map {it.target.id},
      infos = targets.map {
        BaseTargetInfo(it.target, it.sources, it.resources)
      },
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

  private fun generateTarget(
    targetId: BuildTargetIdentifier,
    type: String,
    dependencies: List<BuildTargetIdentifier> = listOf(),
    sourcesItems: List<String> = listOf(),
    resourcesItems: List<String> = listOf(),
  ): BaseTargetInfo {
    val target = BuildTarget(
      targetId,
      listOf(type),
      listOf("go"),
      dependencies,
      BuildTargetCapabilities())
    val sources = sourcesItems.map {SourcesItem(targetId, listOf(SourceItem(it, SourceItemKind.forValue(1), false)))}
    val resources = resourcesItems.map {ResourcesItem(targetId, listOf(it))}
    return BaseTargetInfo(target, sources, resources)
  }
}

