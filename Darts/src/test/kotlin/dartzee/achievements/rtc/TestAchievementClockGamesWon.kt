package dartzee.test.achievements.rtc

import dartzee.achievements.rtc.AchievementClockGamesWon
import dartzee.db.GAME_TYPE_ROUND_THE_CLOCK
import dartzee.test.achievements.TestAbstractAchievementGamesWon
import io.kotlintest.shouldBe
import org.junit.Test


class TestAchievementClockGamesWon: TestAbstractAchievementGamesWon<AchievementClockGamesWon>()
{
    override fun factoryAchievement() = AchievementClockGamesWon()

    @Test
    fun `Game type should be correct`()
    {
        factoryAchievement().gameType shouldBe GAME_TYPE_ROUND_THE_CLOCK
    }
}