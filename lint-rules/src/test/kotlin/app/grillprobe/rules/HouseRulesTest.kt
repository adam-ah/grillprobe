package app.grillprobe.rules

import io.gitlab.arturbosch.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Test

class HouseRulesTest {
    @Test
    fun reportsLineComments() {
        assertEquals(1, NoComments().lint("val answer = 42 // the answer").size)
    }

    @Test
    fun reportsBlockAndDocComments() {
        val code =
            """
            /** Documented. */
            class Documented {
                /* block */
                val value = 1
            }
            """.trimIndent()
        assertEquals(2, NoComments().lint(code).size)
    }

    @Test
    fun ignoresCommentMarkersInsideStrings() {
        assertEquals(0, NoComments().lint("val url = \"https://example.com/*path*/\"").size)
    }

    @Test
    fun allowsFilesUpToTheLimit() {
        assertEquals(0, MaxFileLength().lint(lines(400)).size)
    }

    @Test
    fun reportsFilesOverTheLimit() {
        assertEquals(1, MaxFileLength().lint(lines(401)).size)
    }

    private fun lines(count: Int) = (1..count).joinToString("\n") { "val v$it = $it" }
}
