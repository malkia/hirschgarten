load("@//server/rules/kotlin:junit5.bzl", "kt_test")

kt_test(
    name = "ProjectViewRawSectionsTest",
    size = "small",
    src = "ProjectViewRawSectionsTest.kt",
    deps = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/parser/splitter",
    ],
)

kt_test(
    name = "ProjectViewSectionSplitterTest",
    size = "small",
    src = "ProjectViewSectionSplitterTest.kt",
    deps = [
        "//server/executioncontext/projectview/src/main/kotlin/org/jetbrains/bsp/bazel/projectview/parser/splitter",
    ],
)
