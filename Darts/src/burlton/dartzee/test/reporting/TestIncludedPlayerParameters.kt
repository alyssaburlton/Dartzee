package burlton.dartzee.test.reporting

import burlton.dartzee.code.reporting.IncludedPlayerParameters
import burlton.dartzee.code.reporting.ReportParameters
import burlton.dartzee.code.reporting.runReport
import burlton.dartzee.test.helper.*
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.string.shouldBeEmpty
import org.junit.Test

class TestIncludedPlayerParameters: AbstractDartsTest()
{
    override fun beforeEachTest()
    {
        super.beforeEachTest()

        wipeTable("Game")
    }

    @Test
    fun `Should be blank by default`()
    {
        val ipp = IncludedPlayerParameters()
        ipp.generateExtraWhereSql("foo").shouldBeEmpty()
    }

    @Test
    fun `Should filter by finishing position`()
    {
        val player = insertPlayer("Bob")

        val winningGameId = insertGame(localId = 1).rowId
        val thirdGameId = insertGame(localId = 2).rowId
        val secondGameId = insertGame(localId = 3).rowId
        val unfinishedGameId = insertGame(localId = 4).rowId

        insertParticipant(playerId = player.rowId, finishingPosition = 1, gameId = winningGameId)
        insertParticipant(playerId = player.rowId, finishingPosition = 2, gameId = secondGameId)
        insertParticipant(playerId = player.rowId, finishingPosition = 3, gameId = thirdGameId)
        insertParticipant(playerId = player.rowId, finishingPosition = -1, gameId = unfinishedGameId)

        val ipp = IncludedPlayerParameters()
        ipp.finishingPositions = listOf(1, 3)

        val rp = ReportParameters()
        rp.hmIncludedPlayerToParms[player] = ipp

        val results = runReport(rp)
        results shouldHaveSize 2

        results.map{ it.localId }.shouldContainExactlyInAnyOrder(1, 2)
    }
}