package app.grillprobe.rules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class HouseRulesProvider : RuleSetProvider {
    override val ruleSetId: String = "house"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(MaxFileLength(config), NoComments(config))
    )
}
