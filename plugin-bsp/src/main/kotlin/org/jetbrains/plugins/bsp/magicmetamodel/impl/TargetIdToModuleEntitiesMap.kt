package org.jetbrains.plugins.bsp.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfoOld
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import java.nio.file.Path

internal object TargetIdToModuleEntitiesMap {
  operator fun invoke(
    projectDetails: ProjectDetails,
    targetIdToModuleDetails: Map<BuildTargetIdentifier, ModuleDetails>,
    targetIdToTargetInfo: Map<BuildTargetIdentifier, BuildTargetInfoOld>,
    projectBasePath: Path,
    targetsMap: Map<BuildTargetId, BuildTargetInfoOld>,
    moduleNameProvider: TargetNameReformatProvider,
    libraryNameProvider: TargetNameReformatProvider,
    isAndroidSupportEnabled: Boolean,
  ): Map<BuildTargetIdentifier, Module> {
    val moduleDetailsToJavaModuleTransformer = ModuleDetailsToJavaModuleTransformer(
      targetIdToTargetInfo,
      moduleNameProvider,
      libraryNameProvider,
      projectBasePath,
      isAndroidSupportEnabled,
    )

    return emptyMap()
//    return runBlocking(Dispatchers.Default) {
//      projectDetails.targetIds.map {
//        async {
////          val moduleDetails = transformer.moduleDetailsForTargetId(it)
////          if (moduleDetails.target.languageIds.includesPython()) {
////            null
////          val module = targetIdToModuleDetails.getValue(it)
////          it to module
//        }
//      }.awaitAll().filterNotNull().toMap()
//    }
  }
}

@TestOnly
public fun Collection<String>.toDefaultTargetsMap(): Map<BuildTargetIdentifier, BuildTargetInfoOld> =
  associateBy(
    keySelector = { BuildTargetIdentifier(it) },
    valueTransform = { BuildTargetInfoOld(id = it) }
  )
