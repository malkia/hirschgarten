package configurations

import jetbrains.buildServer.configs.kotlin.v10.toExtId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot


open class Aggregator (
    vcsRoot: GitVcsRoot,
): BuildType({

    name = "results"

    vcs {
        root(BaseConfiguration.GitHubVcs)
        showDependenciesChanges = false
    }

    allowExternalStatus = true

    if (vcsRoot.name == "intellij-bsp-github") {
        id(("GitHub_" + name).toExtId())
        features {
            pullRequests {
                vcsRootExtId = "${BaseConfiguration.GitHubVcs.id}"
                provider = github {
                    authType = token {
                        token = "credentialsJSON:5bc345d4-e38f-4428-95e1-b6e4121aadf6"
                    }
                    filterAuthorRole = PullRequests.GitHubRoleFilter.EVERYBODY
                }
            }
        }
    } else {
        id(("Space_" + name).toExtId())
    }

    type = Type.COMPOSITE
})

object GitHub : Aggregator(
    vcsRoot = BaseConfiguration.GitHubVcs,
)

object Space : Aggregator(
    vcsRoot = BaseConfiguration.SpaceVcs
)