package org.jetbrains.plugins.bsp.magicmetamodel.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToJavaModuleTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ModuleDetailsToPythonModuleTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesPython
import java.nio.file.Path

internal object TargetIdToModuleEntitiesMap {
  operator fun invoke(
    projectDetails: ProjectDetails,
    targetIdToModuleDetails: Map<BuildTargetIdentifier, ModuleDetails>,
    targetIdToTargetInfo: Map<BuildTargetIdentifier, BuildTargetInfo>,
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
    val moduleDetailsToPythonModuleTransformer = ModuleDetailsToPythonModuleTransformer(
      targetIdToTargetInfo,
      moduleNameProvider,
      libraryNameProvider,
      hasDefaultPythonInterpreter,
    )

    return runBlocking(Dispatchers.Default) {
      projectDetails.targetIds.map {
        async {
          val moduleDetails = transformer.moduleDetailsForTargetId(it)
          if (moduleDetails.target.languageIds.includesPython()) {
            null
          val moduleDetails = targetIdToModuleDetails.getValue(it)
          val module = if (moduleDetails.target.languageIds.includesPython()) {
            moduleDetailsToPythonModuleTransformer.transform(moduleDetails)
          } else {
            it.uri to moduleDetailsToJavaModuleTransformer.transform(moduleDetails)
          }
          it to module
        }
      }.awaitAll().filterNotNull().toMap()
    }
  }
}

@TestOnly
public fun Collection<String>.toDefaultTargetsMap(): Map<BuildTargetId, BuildTargetInfoOld> =
  associateBy(
    keySelector = { it },
    valueTransform = { BuildTargetInfoOld(id = it) }
  )
