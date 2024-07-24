package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import java.net.URI

public data class LibraryItem(
  val id: BuildTargetIdentifier,
  val dependencies: List<BuildTargetIdentifier>,
  val ijars: List<String>,
  val jars: List<String>,
  val sourceJars: List<String>,
  val goImportPath: String? = null,
  val goRoot: URI? = null,
)

public data class WorkspaceLibrariesResult(
  val libraries: List<LibraryItem>,
)
