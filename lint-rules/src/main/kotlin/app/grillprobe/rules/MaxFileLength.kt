package app.grillprobe.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtFile

class MaxFileLength(
    config: Config = Config.empty,
) : Rule(config) {
    override val issue: Issue =
        Issue(
            javaClass.simpleName,
            Severity.Maintainability,
            "Source files must stay short. Split the file into smaller units.",
            Debt.TWENTY_MINS,
        )

    private val maxLines: Int by lazy { valueOrDefault("maxLines", DEFAULT_MAX_LINES) }

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        val lines = file.text.trimEnd('\n').lines().size
        if (lines > maxLines) {
            report(
                CodeSmell(issue, Entity.atPackageOrFirstDecl(file), "File has $lines lines, the limit is $maxLines.")
            )
        }
    }

    private companion object {
        const val DEFAULT_MAX_LINES = 400
    }
}
