package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.module.impl.ModuleManagerEx
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleCustomImlDataEntity
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.customImlData
import com.intellij.platform.workspace.jps.entities.modifyEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.workspaceModel.ide.impl.LegacyBridgeJpsEntitySourceFactory
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsConstants
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsFeatureFlags
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsPaths
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.IntermediateLibraryDependency
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.IntermediateModuleDependency
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.workspacemodel.entities.BspDummyEntitySource
import org.jetbrains.workspacemodel.entities.BspEntitySource

internal class ModuleEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val defaultDependencies: List<ModuleDependencyItem> = ArrayList(),
) : WorkspaceModelEntityWithoutParentModuleUpdater<GenericModuleInfo, ModuleEntity> {
  override fun addEntity(entityToAdd: GenericModuleInfo): ModuleEntity =
    addModuleEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, entityToAdd)

  private fun addModuleEntity(
    builder: MutableEntityStorage,
    entityToAdd: GenericModuleInfo,
  ): ModuleEntity {
    val associatesDependencies = entityToAdd.associates.map { toModuleDependencyItemModuleDependency(it) }
    val (libraryModulesDependencies, librariesDependencies) =
      entityToAdd.librariesDependencies.partition {
        !entityToAdd.isLibraryModule &&
          workspaceModelEntityUpdaterConfig.project.temporaryTargetUtils.isLibraryModule(it.libraryName)
      }

    val modulesDependencies =
      (entityToAdd.modulesDependencies + libraryModulesDependencies.toLibraryModuleDependencies()).map {
        toModuleDependencyItemModuleDependency(it)
      }

    // library dependencies should be included before module dependencies
    // to handle the case of overridden library versions
    val dependencies =
      defaultDependencies +
        librariesDependencies.map { toLibraryDependency(it) } +
        modulesDependencies +
        associatesDependencies

    val moduleEntity = builder.addEntity(
      ModuleEntity(
        name = entityToAdd.name,
        dependencies = dependencies,
        entitySource = toEntitySource(entityToAdd),
      ) {
        this.type = entityToAdd.type
      },
    )
    val imlData = builder.addEntity(
      ModuleCustomImlDataEntity(
        customModuleOptions = entityToAdd.capabilities.asMap() + entityToAdd.languageIdsAsSingleEntryMap,
        entitySource = moduleEntity.entitySource,
      ) {
        this.rootManagerTagCustomData = null
        this.module = moduleEntity
      },
    )
    builder.modifyEntity(moduleEntity) {
      this.customImlData = imlData
    }
    return moduleEntity
  }

  private fun List<IntermediateLibraryDependency>.toLibraryModuleDependencies() =
    this.map { IntermediateModuleDependency(it.libraryName) }

  private fun toEntitySource(entityToAdd: GenericModuleInfo): EntitySource = when {
    entityToAdd.isDummy -> BspDummyEntitySource
    !JpsFeatureFlags.isJpsCompilationEnabled ||
      entityToAdd.languageIds.any { it !in JpsConstants.SUPPORTED_LANGUAGES } -> BspEntitySource

    else -> LegacyBridgeJpsEntitySourceFactory.createEntitySourceForModule(
      project = workspaceModelEntityUpdaterConfig.project,
      baseModuleDir = JpsPaths.getJpsImlModulesPath(workspaceModelEntityUpdaterConfig.projectBasePath)
        .toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
      externalSource = null,
      moduleFileName = entityToAdd.name + ModuleManagerEx.IML_EXTENSION
    )
  }

  private fun toModuleDependencyItemModuleDependency(
    intermediateModuleDependency: IntermediateModuleDependency,
  ): ModuleDependency =
    ModuleDependency(
      module = ModuleId(intermediateModuleDependency.moduleName),
      exported = true,
      scope = DependencyScope.COMPILE,
      productionOnTest = true,
    )
}

internal fun toLibraryDependency(intermediateLibraryDependency: IntermediateLibraryDependency): LibraryDependency =
  LibraryDependency(
    library = LibraryId(
      name = intermediateLibraryDependency.libraryName,
      tableId = LibraryTableId.ProjectLibraryTableId, // treat all libraries as project-level libraries
    ),
    exported = true, // TODO https://youtrack.jetbrains.com/issue/BAZEL-632
    scope = DependencyScope.COMPILE,
  )
