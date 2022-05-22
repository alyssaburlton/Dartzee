package dartzee.screen.game

import dartzee.db.DartsMatchEntity
import dartzee.getRows
import dartzee.helper.AbstractTest
import dartzee.helper.insertDartsMatch
import dartzee.helper.insertPlayer
import dartzee.screen.game.x01.GameStatisticsPanelX01
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class TestMatchSummaryPanel : AbstractTest()
{
    @Test
    fun `Should correctly assign scorers and then reuse for existing participants`()
    {
        val panel = makeMatchSummaryPanel()

        val p1 = insertPlayer(name = "Alyssa")
        val p2 = insertPlayer(name = "Leah")
        val p3 = insertPlayer(name = "Qwi")
        val p4 = insertPlayer(name = "Natalie")

        val alyssaAndLeah = makeTeam(p1, p2)
        val qwiAndNatalie = makeTeam(p3, p4)

        panel.addParticipant(1, alyssaAndLeah)
        panel.addParticipant(1, qwiAndNatalie)
        panel.scorersOrdered.size shouldBe 2

        val leahAndAlyssa = makeTeam(p2, p1)
        val natalieAndQwi = makeTeam(p4, p3)
        panel.addParticipant(2, natalieAndQwi)
        panel.addParticipant(2, leahAndAlyssa)
        panel.scorersOrdered.size shouldBe 2

        val firstScorer = panel.scorersOrdered[0]
        val rows = firstScorer.model.getRows(4)
        rows[0] shouldBe listOf(1L, alyssaAndLeah, alyssaAndLeah, alyssaAndLeah)
        rows[1] shouldBe listOf(2L, leahAndAlyssa, leahAndAlyssa, leahAndAlyssa)

        val secondScorer = panel.scorersOrdered[1]
        val otherRows = secondScorer.model.getRows(4)
        otherRows[0] shouldBe listOf(1L, qwiAndNatalie, qwiAndNatalie, qwiAndNatalie)
        otherRows[1] shouldBe listOf(2L, natalieAndQwi, natalieAndQwi, natalieAndQwi)
    }

    private fun makeMatchSummaryPanel(
        match: DartsMatchEntity = insertDartsMatch(),
        statsPanel: GameStatisticsPanelX01 = GameStatisticsPanelX01("501"))
    = MatchSummaryPanel(match, statsPanel)
}