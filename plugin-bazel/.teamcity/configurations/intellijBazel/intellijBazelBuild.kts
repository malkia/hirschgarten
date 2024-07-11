package configurations.intellijBazel

import configurations.BaseConfiguration
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.bazel
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

open class Build (
    vcsRoot: GitVcsRoot,
): BaseConfiguration.BaseBuildType(
        name = "[build] build intellij-bazel",
        artifactRules = "+:%system.teamcity.build.checkoutDir%/bazel-bin/intellij-bazel.zip",
        vcsRoot = vcsRoot,
        steps = {
            bazel {
                id = "build_plugin"
                name = "build plugin"
                command = "build"
                targets = "//..."
                param("toolPath", "/usr/local/bin")
            }
        },
)

object GitHub : Build(
    vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : Build(
    vcsRoot = BaseConfiguration.SpaceVcs
)