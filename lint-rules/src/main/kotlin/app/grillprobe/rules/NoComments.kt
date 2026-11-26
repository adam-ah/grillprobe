package app.grillprobe.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement

class NoComments(
    config: Config = Config.empty,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            javaClass.simpleName,
            Severity.Style,
            "Comments are not allowed in source code. Express intent through naming and structure.",
            Debt.FIVE_MINS,
        )

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        if (element is PsiComment) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(element),
                    "Remove this comment: ${element.text.lineSequence().first().take(PREVIEW)}"
                )
            )
        }
    }

    private companion object {
        const val PREVIEW = 60
    }
}
