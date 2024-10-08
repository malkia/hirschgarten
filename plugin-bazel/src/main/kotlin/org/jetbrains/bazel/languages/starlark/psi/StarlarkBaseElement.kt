package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.bazel.languages.bazel.BazelPackage

abstract class StarlarkBaseElement(node: ASTNode) :
  ASTWrapperPsiElement(node),
  StarlarkElement {
  override fun accept(visitor: PsiElementVisitor) {
    if (visitor is StarlarkElementVisitor) {
      acceptVisitor(visitor)
    } else {
      super.accept(visitor)
    }
  }

  protected abstract fun acceptVisitor(visitor: StarlarkElementVisitor)

  fun getBazelPackage(): BazelPackage? = BazelPackage.ofFile(containingFile as StarlarkFile)
}
