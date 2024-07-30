package org.jetbrains.plugins.bsp.run.commandLine

fun transformProgramArguments(input: String?): List<String> =
  listOfNotNull(input) // TODO: figure out how to split the arguments (if at all)
