package dartzee.screen.game.rtc

import dartzee.core.util.minOrZero
import dartzee.screen.game.rtc.GameStatisticsPanelRoundTheClock

class MatchStatisticsPanelRoundTheClock : GameStatisticsPanelRoundTheClock()
{
    override fun addRowsToTable()
    {
        super.addRowsToTable()

        addRow(getBestGameRow { it.minOrZero() })
        addRow(getAverageGameRow())
    }

    override fun getRankedRowsLowestWins(): MutableList<String>
    {
        val ret = super.getRankedRowsLowestWins()
        ret.add("Best Game")
        ret.add("Avg Game")
        return ret
    }
}