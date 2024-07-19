package org.jetbrains.plugins.bsp.projectStructure

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

public interface ProjectStructureDiff {
  public suspend fun apply(project: Project)
}

public class AllProjectStructuresDiff(
  private val project: Project,
  diffs: List<ProjectStructureDiff>,
) {
  private val diffs = diffs.associateBy { it::class.java }

  @Suppress("UNCHECKED_CAST")
  public fun <TDiff: ProjectStructureDiff>diffOfType(diffClazz: Class<TDiff>): TDiff =
    diffs[diffClazz] as? TDiff ?: error("Cannot find a ProjectStructureDiff of type: ${diffClazz.simpleName}")

  internal suspend fun applyAll() {
    diffs.values.forEach { it.apply(project) }
  }
}

public interface ProjectStructureProvider<TDiff : ProjectStructureDiff, TCurrent> {
  public fun newDiff(project: Project): TDiff
  public fun current(project: Project): TCurrent

  public companion object {
    internal val ep = ExtensionPointName.create<ProjectStructureProvider<*, *>>("org.jetbrains.bsp.projectStructureProvider")
  }
}

internal class AllProjectStructuresProvider(private val project: Project) {
  fun newDiff(): AllProjectStructuresDiff {
    val providers = ProjectStructureProvider.ep.extensionList
    val diffs = providers.map { it.newDiff(project) }
//    val updaters = ProjectStructureUpdater.ep.extensionList

    return AllProjectStructuresDiff(project, diffs)
  }
}
