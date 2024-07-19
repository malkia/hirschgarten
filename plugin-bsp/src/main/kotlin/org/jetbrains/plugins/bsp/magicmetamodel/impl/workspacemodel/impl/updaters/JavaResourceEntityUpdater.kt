package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.java.workspace.entities.JavaResourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaResourceRoots
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.modifyContentRootEntity
import com.intellij.platform.workspace.jps.entities.modifySourceRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ResourceRoot

internal class JavaResourceEntityUpdater(
    private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<ResourceRoot, JavaResourceRootPropertiesEntity> {
  private val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntities(
      entitiesToAdd: List<ResourceRoot>,
      parentModuleEntity: ModuleEntity
  ): List<JavaResourceRootPropertiesEntity> {
    val contentRootEntities = addContentRootEntities(entitiesToAdd, parentModuleEntity)

    val sourceRoots =
        (entitiesToAdd zip contentRootEntities).map { (entityToAdd, contentRootEntity) ->
          addSourceRootEntity(
              workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder,
              contentRootEntity,
              entityToAdd,
              parentModuleEntity,
          )
        }
    return sourceRoots.map { sourceRoot ->
      addJavaResourceRootEntity(
          workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, sourceRoot)
    }
  }

  private fun addContentRootEntities(
      entitiesToAdd: List<ResourceRoot>,
      parentModuleEntity: ModuleEntity,
  ): List<ContentRootEntity> {
    val contentRoots =
        entitiesToAdd.map { entityToAdd ->
          ContentRoot(
              path = entityToAdd.resourcePath,
          )
        }

    return contentRootEntityUpdater.addEntities(contentRoots, parentModuleEntity)
  }

  private fun addSourceRootEntity(
      builder: MutableEntityStorage,
      contentRootEntity: ContentRootEntity,
      entityToAdd: ResourceRoot,
      parentModuleEntity: ModuleEntity,
  ): SourceRootEntity {
    val entity =
        SourceRootEntity(
            url =
                entityToAdd.resourcePath.toVirtualFileUrl(
                    workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
            rootTypeId = entityToAdd.rootType,
            entitySource = parentModuleEntity.entitySource,
        )

    val updatedContentRootEntity =
        builder.modifyContentRootEntity(contentRootEntity) { this.sourceRoots += entity }

    return updatedContentRootEntity.sourceRoots.last()
  }

  private fun addJavaResourceRootEntity(
      builder: MutableEntityStorage,
      sourceRoot: SourceRootEntity,
  ): JavaResourceRootPropertiesEntity {
    val entity =
        JavaResourceRootPropertiesEntity(
            generated = DEFAULT_GENERATED,
            relativeOutputPath = DEFAULT_RELATIVE_OUTPUT_PATH,
            entitySource = sourceRoot.entitySource,
        )

    val updatedSourceRoot =
        builder.modifySourceRootEntity(sourceRoot) { this.javaResourceRoots += entity }

    return updatedSourceRoot.javaResourceRoots.last()
  }

  override fun addEntity(
      entityToAdd: ResourceRoot,
      parentModuleEntity: ModuleEntity
  ): JavaResourceRootPropertiesEntity {
    return addEntities(listOf(entityToAdd), parentModuleEntity).single()
  }

  private companion object {
    private const val DEFAULT_GENERATED = false
    private const val DEFAULT_RELATIVE_OUTPUT_PATH = ""
  }
}
