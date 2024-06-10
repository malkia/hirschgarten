package org.jetbrains.plugins.bsp.projectStructure

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.project.Project
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private abstract class TestableProjectStructureDiffBase : ProjectStructureDiff {
  val updates: MutableList<BuildTargetIdentifier> = mutableListOf()
  var hasBeenApplied: Boolean = false

  override suspend fun apply(project: Project) {
    hasBeenApplied = true
  }
}

private class TestableProjectStructureDiff : TestableProjectStructureDiffBase()
private class AnotherTestableProjectStructureDiff : TestableProjectStructureDiffBase()

private abstract class TestableProjectStructureUpdaterBase<T: TestableProjectStructureDiffBase>(
  private val unsupportedTargets: List<BuildTargetIdentifier>
) : ProjectStructureUpdater<T> {
  override fun isSupported(buildTarget: BuildTarget): Boolean =
    buildTarget.id !in unsupportedTargets

  override fun addTarget(targetInfo: BuildTargetInfo, diff: T) {
    diff.updates.add(targetInfo.target.id)
  }
}

private class TestableProjectStructureUpdater(
  unsupportedTargets: List<BuildTargetIdentifier>
) : TestableProjectStructureUpdaterBase<TestableProjectStructureDiff>(unsupportedTargets) {
  override val diffClass: Class<TestableProjectStructureDiff> = TestableProjectStructureDiff::class.java
}

private class AnotherTestableProjectStructureUpdater(
  unsupportedTargets: List<BuildTargetIdentifier>
) : TestableProjectStructureUpdaterBase<AnotherTestableProjectStructureDiff>(unsupportedTargets) {
  override val diffClass: Class<AnotherTestableProjectStructureDiff> = AnotherTestableProjectStructureDiff::class.java
}


@DisplayName("AllProjectStructuresDiff tests")
class AllProjectStructuresDiffTest : MockProjectBaseTest() {
  @Test
  fun `should not apply changes if applyAll was not called`() {
    // given
    val targetId = BuildTargetIdentifier("target")

    val updater = TestableProjectStructureUpdater(emptyList())
    val diff = TestableProjectStructureDiff()
    val allDiff = AllProjectStructuresDiff(listOf(diff), listOf(updater), project)

    val targetInfo = targetId.toMockBuildTargetInfo()

    // when
    allDiff.addTarget(targetInfo)

    // then
    diff.hasBeenApplied shouldBe false
  }

  @Test
  fun `should not add not supported target`() {
    // given
    val targetId = BuildTargetIdentifier("not supported target")

    val updater = TestableProjectStructureUpdater(listOf(targetId))
    val diff = TestableProjectStructureDiff()
    val allDiff = AllProjectStructuresDiff(listOf(diff), listOf(updater), project)

    val targetInfo = targetId.toMockBuildTargetInfo()

    // when
    allDiff.addTarget(targetInfo)

    runWriteAction {
      runBlocking {
        allDiff.applyAll()
      }
    }

    // then
    diff.hasBeenApplied shouldBe true
    diff.updates shouldBe emptyList()
  }

  @Test
  fun `should add a supported target`() {
    // given
    val targetId = BuildTargetIdentifier("supported target")

    val updater = TestableProjectStructureUpdater(emptyList())
    val diff = TestableProjectStructureDiff()
    val allDiff = AllProjectStructuresDiff(listOf(diff), listOf(updater), project)

    val targetInfo = targetId.toMockBuildTargetInfo()

    // when
    allDiff.addTarget(targetInfo)

    runWriteAction {
      runBlocking {
        allDiff.applyAll()
      }
    }

    // then
    diff.hasBeenApplied shouldBe true
    diff.updates shouldContainExactlyInAnyOrder listOf(targetId)
  }

  @Test
  fun `should add multiple target to multiple diffs`() {
    // given
    val target1Id = BuildTargetIdentifier("target1 diff 1")
    val target2Id = BuildTargetIdentifier("target2 diff 1")
    val target3Id = BuildTargetIdentifier("target3 diff 1 and 2")
    val target4Id = BuildTargetIdentifier("target4 diff 2")
    val target5Id = BuildTargetIdentifier("target5 not supported at all")

    val updater1 = TestableProjectStructureUpdater(listOf(target4Id, target5Id))
    val updater2 = AnotherTestableProjectStructureUpdater(listOf(target1Id, target2Id, target5Id))

    val diff1 = TestableProjectStructureDiff()
    val diff2 = AnotherTestableProjectStructureDiff()
    val allDiff = AllProjectStructuresDiff(listOf(diff1, diff2), listOf(updater1, updater2), project)

    val targetInfo1 = target1Id.toMockBuildTargetInfo()
    val targetInfo2 = target2Id.toMockBuildTargetInfo()
    val targetInfo3 = target3Id.toMockBuildTargetInfo()
    val targetInfo4 = target4Id.toMockBuildTargetInfo()
    val targetInfo5 = target5Id.toMockBuildTargetInfo()

    // when
    allDiff.addTarget(targetInfo1)
    allDiff.addTarget(targetInfo2)
    allDiff.addTarget(targetInfo3)
    allDiff.addTarget(targetInfo4)
    allDiff.addTarget(targetInfo5)

    runWriteAction {
      runBlocking {
        allDiff.applyAll()
      }
    }

    // then
    diff1.hasBeenApplied shouldBe true
    diff1.updates shouldContainExactlyInAnyOrder listOf(target1Id, target2Id, target3Id)

    diff2.hasBeenApplied shouldBe true
    diff2.updates shouldContainExactlyInAnyOrder listOf(target3Id, target4Id)
  }

  private fun BuildTargetIdentifier.toMockBuildTargetInfo(): BuildTargetInfo =
    BuildTargetInfo(
      target = BuildTarget(this, emptyList(), emptyList(), emptyList(), BuildTargetCapabilities()),
    )
}
