#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
licenses(["notice"])

java_plugin(
    name = "auto_annotation_plugin",
    processor_class = "com.google.auto.value.processor.AutoAnnotationProcessor",
    deps = ["@auto_value//jar"],
)

java_plugin(
    name = "auto_oneof_plugin",
    processor_class = "com.google.auto.value.processor.AutoOneOfProcessor",
    deps = ["@auto_value//jar"],
)

java_plugin(
    name = "auto_value_plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    visibility = ["//visibility:public"],  # FastBuildCompilerFactoryImplTest uses this
    deps = ["@auto_value//jar"],
)

# provides both the jar for compilation and the java_plugin.
java_library(
    name = "auto_value",
    exported_plugins = [
        ":auto_annotation_plugin",
        ":auto_oneof_plugin",
        ":auto_value_plugin",
    ],
    visibility = ["//visibility:public"],
    exports = ["@auto_value_annotations//jar"],
)

# You should only need to use this library in rare situations, such as testing
# other annotations processors that generate @AutoValue types. If you just want
# to use @AutoValue, you should depend on the auto_value library.
java_library(
    name = "auto_value_processor",
    visibility = ["//visibility:public"],
    exports = ["@auto_value//jar"],
)
