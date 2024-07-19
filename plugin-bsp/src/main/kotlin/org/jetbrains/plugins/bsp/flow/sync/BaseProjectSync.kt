package org.jetbrains.plugins.bsp.flow.sync

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.JoinedBuildServer
import java.util.concurrent.CompletableFuture

data class BaseTargetInfos(
  val allTargetIds: List<BuildTargetIdentifier>,
  val infos: List<BaseTargetInfo>,
)

data class BaseTargetInfo(
  val target: BuildTarget,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
)

class BaseProjectSync(private val project: Project) {
  fun execute(
    buildProject: Boolean,
    server: JoinedBuildServer,
    capabilities: BuildServerCapabilities,
    cancelOn: CompletableFuture<Void>,
    errorCallback: (Throwable) -> Unit,
  ): BaseTargetInfos {
    val buildTargets = server.queryWorkspaceBuildTargets(buildProject, cancelOn, errorCallback)
    val allTargetIds = buildTargets.calculateAllTargetIds()

    val sourcesFuture = server.querySources(allTargetIds, cancelOn, errorCallback)
    val resourcesFuture = server.queryResources(allTargetIds, capabilities, cancelOn, errorCallback)

    return BaseTargetInfos(
      allTargetIds = allTargetIds,
      infos = calculateBaseTargetInfos(buildTargets, sourcesFuture, resourcesFuture)
    )
  }

  private fun JoinedBuildServer.queryWorkspaceBuildTargets(
    buildProject: Boolean,
    cancelOn: CompletableFuture<Void>,
    errorCallback: (Throwable) -> Unit,
  ): List<BuildTarget> {
    val resultFuture =
      if (buildProject) query("workspace/buildAndGetBuildTargets", cancelOn, errorCallback) { workspaceBuildAndGetBuildTargets() }
      else query("workspace/buildAndGetBuildTargets", cancelOn, errorCallback) { workspaceBuildAndGetBuildTargets() }

    return resultFuture.get().targets
  }

  private fun List<BuildTarget>.calculateAllTargetIds(): List<BuildTargetIdentifier> = map { it.id }

  private fun JoinedBuildServer.querySources(
    targetIds: List<BuildTargetIdentifier>,
    cancelOn: CompletableFuture<Void>,
    errorCallback: (Throwable) -> Unit,
  ): CompletableFuture<SourcesResult> =
    query("buildTarget/sources", cancelOn, errorCallback) { buildTargetSources(SourcesParams(targetIds)) }

  private fun JoinedBuildServer.queryResources(
    targetIds: List<BuildTargetIdentifier>,
    capabilities: BuildServerCapabilities,
    cancelOn: CompletableFuture<Void>,
    errorCallback: (Throwable) -> Unit,
  ): CompletableFuture<ResourcesResult>? =
    queryIf(capabilities.resourcesProvider == true, "buildTarget/resources", cancelOn, errorCallback) {
      buildTargetResources(ResourcesParams(targetIds))
    }

  private fun calculateBaseTargetInfos(
    buildTargets: List<BuildTarget>,
    sourcesFuture: CompletableFuture<SourcesResult>,
    resourcesFuture: CompletableFuture<ResourcesResult>?
  ): List<BaseTargetInfo> {
    val sourcesIndex = sourcesFuture.toSourcesIndex()
    val resourcesIndex = resourcesFuture?.toResourcesIndex().orEmpty()

    return buildTargets.map {
      BaseTargetInfo(
        target = it,
        sources = sourcesIndex[it.id].orEmpty(),
        resources = resourcesIndex[it.id].orEmpty(),
      )
    }
  }

  private fun CompletableFuture<SourcesResult>.toSourcesIndex(): Map<BuildTargetIdentifier, List<SourcesItem>> =
    get().items.orEmpty().groupBy { it.target }

  private fun CompletableFuture<ResourcesResult>.toResourcesIndex(): Map<BuildTargetIdentifier, List<ResourcesItem>> =
    get()?.items.orEmpty().groupBy { it.target }
}
