package org.jetbrains.plugins.bsp.ui.configuration.run

import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration

public class GenericBspRunHandlerProvider : BspRunHandlerProvider {
  override val id: String = "GenericBspRunHandlerProvider"

  override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler {
    return GenericBspRunHandler()
  }

  override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean = targetInfos.singleOrNull()?.capabilities?.canRun ?: false
  override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = false // TODO: Figure out debugging in the generic case
}