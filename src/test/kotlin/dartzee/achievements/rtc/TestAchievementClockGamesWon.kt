package dartzee.achievements.rtc

import dartzee.achievements.TestAbstractAchievementGamesWon
import dartzee.db.GameType
import io.kotlintest.shouldBe
import org.junit.Test


class TestAchievementClockGamesWon: TestAbstractAchievementGamesWon<AchievementClockGamesWon>()
{
    override fun factoryAchievement() = AchievementClockGamesWon()

    @Test
    fun `Game type should be correct`()
    {
        factoryAchievement().gameType shouldBe GameType.ROUND_THE_CLOCK
    }
}