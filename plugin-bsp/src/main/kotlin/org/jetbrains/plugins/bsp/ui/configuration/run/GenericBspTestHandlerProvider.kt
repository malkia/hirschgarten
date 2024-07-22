package org.jetbrains.plugins.bsp.ui.configuration.run

import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration

public class GenericBspTestHandlerProvider : BspRunHandlerProvider {
  override val id: String = "GenericBspTestHandlerProvider"

  override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler {
    return GenericBspTestHandler()
  }

  override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean = targetInfos.all { it.capabilities.canTest }
  override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = false
}