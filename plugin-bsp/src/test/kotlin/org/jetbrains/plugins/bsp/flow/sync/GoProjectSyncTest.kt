package org.jetbrains.plugins.bsp.flow.sync

import ch.epfl.scala.bsp4j.*
import com.goide.vgo.project.workspaceModel.entities.VgoDependencyEntity
import com.goide.vgo.project.workspaceModel.entities.VgoStandaloneModuleEntity
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bsp.protocol.*
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresProvider
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.workspacemodel.entities.BspEntitySource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.concurrent.CompletableFuture
import kotlin.io.path.toPath

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

  private data class GoTestSet(
    val baseTargetInfos: BaseTargetInfos,
    val expectedVgoStandaloneEntities: List<ExpectedVgoStandaloneModuleEntity>,
    val expectedVgoDependencyEntities: List<ExpectedVgoDependencyEntity>,
  )

  private data class ExpectedVgoStandaloneModuleEntity(
    val moduleId: ModuleId,
    val entitySource: EntitySource,
    val importPath: String,
    val root: VirtualFileUrl,
  )

  private data class ExpectedVgoDependencyEntity(
    val importPath: String,
    val entitySource: EntitySource,
    val isMainModule: Boolean,
    val internal: Boolean,
    val module: ExpectedVgoStandaloneModuleEntity,
    val root: VirtualFileUrl
  )

  @Test
  fun `should add VgoStandaloneModuleEntities to workspace model diff`() {
    // given
    val server = GoJoinedBuildServer(PythonOptionsResult(emptyList()))
    val capabilities = BazelBuildServerCapabilities()
    val diff = AllProjectStructuresProvider(project).newDiff()
    val goTestTargets = generateTestSet()

    // when
    runBlocking {
      reportSequentialProgress { reporter ->
        hook.onSync(project, server, capabilities, diff, "test", reporter, goTestTargets.baseTargetInfos, CompletableFuture<Void>().newIncompleteFuture(),{})
      }
    }

    // then
    val actualVgoStandaloneEntitiesResult = diff.workspaceModelDiff.mutableEntityStorage.entities(VgoStandaloneModuleEntity::class.java).toList()
    actualVgoStandaloneEntitiesResult.shouldBeEqual(goTestTargets.expectedVgoStandaloneEntities) {
        actualEntity, expectedEntity -> actualEntity shouldBeEqual expectedEntity
    }
  }

  @Test
  fun `should add dependencies to workspace model diff`() {
    // given
    val server = GoJoinedBuildServer(PythonOptionsResult(emptyList()))
    val capabilities = BazelBuildServerCapabilities()
    val diff = AllProjectStructuresProvider(project).newDiff()
    val goTestTargets = generateTestSet()

    // when
    runBlocking {
      reportSequentialProgress { reporter ->
        hook.onSync(project, server, capabilities, diff, "test", reporter, goTestTargets.baseTargetInfos, CompletableFuture<Void>().newIncompleteFuture(),{})
      }
    }

    // then
    val actualVgoDependencyEntity = diff.workspaceModelDiff.mutableEntityStorage.entities(VgoDependencyEntity::class.java).toList()
    actualVgoDependencyEntity.shouldBeEqual(goTestTargets.expectedVgoDependencyEntities) {
      actualEntity, expectedEntity -> actualEntity shouldBeEqual expectedEntity
    }
  }

//  @Test // TODO
//  fun `should add libraries to workspace model diff`() {
//    // given
//    val server = GoJoinedBuildServer(PythonOptionsResult(emptyList()))
//    val capabilities = BazelBuildServerCapabilities()
//    val diff = AllProjectStructuresProvider(project).newDiff()
//    val goTestTargets = generateTestSet()
//
//    // when
//    runBlocking {
//      reportSequentialProgress { reporter ->
//        hook.onSync(project, server, capabilities, diff, "test", reporter, goTestTargets.baseTargetInfos, CompletableFuture<Void>().newIncompleteFuture(),{})
//      }
//    }
//
//    // then
//    val actualVgoLibraryEntity = diff.workspaceModelDiff.mutableEntityStorage.entities(VgoDependencyEntity::class.java).toList()
//    actualVgoLibraryEntity.shouldBeEqual(goTestTargets.expectedVgoDependencyEntities) {
//        actualEntity, expectedEntity -> actualEntity shouldBeEqual expectedEntity
//    }
//  }

  private data class GeneratedTargetInfo(
    val targetId: BuildTargetIdentifier,
    val type: String,
    val dependencies: List<BuildTargetIdentifier> = listOf(),
    val resourcesItems: List<String> = listOf(),
    val importPath: String
  )

  private fun generateTestSet(): GoTestSet {
    val goLibrary1 = GeneratedTargetInfo(
      targetId = BuildTargetIdentifier("@@server/lib:hello_lib"),
      type = "library",
      importPath = "server/lib/file1.go"
    )
    val goLibrary2 = GeneratedTargetInfo(
      targetId = BuildTargetIdentifier("@server/parser:parser_lib"),
      dependencies = listOf(goLibrary1.targetId),
      type = "library",
      importPath = "server/lib/file1.go"
    )
    val goApplication = GeneratedTargetInfo(
      targetId = BuildTargetIdentifier("@server:main_app"),
      type = "application",
      dependencies = listOf(goLibrary1.targetId, goLibrary2.targetId),
      importPath = "server/main_file.go"
    )

    val targetInfos = listOf(goLibrary1, goLibrary2, goApplication)
    val targets = targetInfos.map {generateTarget(it)}
    val baseTargetInfos = BaseTargetInfos(
      allTargetIds = targets.map {it.target.id},
      infos = targets.map {
        BaseTargetInfo(it.target, it.sources, it.resources)
      },
    )
    val virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()

    val expectedRoot = URI.create("file:///targets_base_dir").toPath().toVirtualFileUrl(virtualFileUrlManager)
    val expectedVgoStandaloneEntities = targetInfos.map {generateVgoStandaloneResult(it, expectedRoot)}
    val expectedVgoDependencyEntities = listOf(
      generateVgoDependencyResult(goLibrary1, goLibrary2, expectedRoot),
      generateVgoDependencyResult(goLibrary1, goApplication, expectedRoot),
      generateVgoDependencyResult(goLibrary2, goApplication, expectedRoot),
    )
    return GoTestSet(baseTargetInfos, expectedVgoStandaloneEntities, expectedVgoDependencyEntities)
  }

  private fun generateTarget(info: GeneratedTargetInfo): BaseTargetInfo {
    val target = BuildTarget(
      info.targetId,
      listOf(info.type),
      listOf("go"),
      info.dependencies,
      BuildTargetCapabilities())
    target.displayName = target.id.toString()
    target.baseDirectory = "file:///targets_base_dir"
    target.dataKind = "go"
    target.data = GoBuildTarget(
      sdkHomePath = URI("file:///go_sdk/"),
      importPath = info.importPath,
    )
    val sources = listOf(SourcesItem(info.targetId, listOf(SourceItem("file///root/$info.importPath", SourceItemKind.forValue(1), false))))
    val resources = info.resourcesItems.map {ResourcesItem(info.targetId, listOf(it))}
    return BaseTargetInfo(target, sources, resources)
  }

  private fun generateVgoStandaloneResult(
    info: GeneratedTargetInfo,
    expectedRoot: VirtualFileUrl,
  ): ExpectedVgoStandaloneModuleEntity =
    ExpectedVgoStandaloneModuleEntity(
      moduleId = ModuleId(info.targetId.toString()),
      entitySource = BspEntitySource,
      importPath = info.importPath,
      root = expectedRoot,
    )

  private fun generateVgoDependencyResult(
    dependencyInfo: GeneratedTargetInfo,
    parentInfo: GeneratedTargetInfo,
    expectedRoot: VirtualFileUrl,
  ): ExpectedVgoDependencyEntity =
    ExpectedVgoDependencyEntity(
      importPath = dependencyInfo.importPath,
      entitySource = BspEntitySource,
      isMainModule = false,
      internal = true,
      module = generateVgoStandaloneResult(parentInfo, expectedRoot),
      root = expectedRoot,
    )

  private inline fun <reified T, reified E> List<T>.shouldBeEqual(expected: List<E>, crossinline compare: (T, E) -> Unit) {
    if (this.size != expected.size) {
      throw AssertionError("Expected size ${expected.size} but got ${this.size}")
    }
    this.zip(expected).forEach { (actualEntity, expectedEntity) -> compare(actualEntity, expectedEntity) }
  }

  private infix fun VgoStandaloneModuleEntity.shouldBeEqual(expected: ExpectedVgoStandaloneModuleEntity) {
    this.moduleId shouldBe expected.moduleId
    this.importPath shouldBe expected.importPath
    this.root shouldBe expected.root
  }

  private infix fun VgoDependencyEntity.shouldBeEqual(expected: ExpectedVgoDependencyEntity) {
    this.importPath shouldBe expected.importPath
    this.isMainModule shouldBe expected.isMainModule
    this.internal shouldBe expected.internal
    this.module?.shouldBeEqual(expected.module)
    this.root shouldBe expected.root
  }
}

