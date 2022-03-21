package dartzee.achievements.dartzee

import dartzee.achievements.AbstractAchievementTest
import dartzee.achievements.AchievementType
import dartzee.db.GameEntity
import dartzee.db.PlayerEntity
import dartzee.helper.insertDartzeeRoundResult
import dartzee.helper.insertParticipant
import dartzee.helper.retrieveAchievement
import dartzee.utils.Database
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import java.sql.Timestamp

class TestAchievementDartzeeHalved: AbstractAchievementTest<AchievementDartzeeHalved>()
{
    override fun factoryAchievement() = AchievementDartzeeHalved()

    override fun setUpAchievementRowForPlayerAndGame(p: PlayerEntity, g: GameEntity, database: Database)
    {
        val pt = insertParticipant(gameId = g.rowId, playerId = p.rowId, database = database)
        insertDartzeeRoundResult(pt, success = false, score = -100, database = database)
    }

    @Test
    fun `Should not include successful rounds`()
    {
        val pt = insertRelevantParticipant()
        insertDartzeeRoundResult(pt, success = true, score = 50)

        factoryAchievement().populateForConversion(emptyList())

        getAchievementCount() shouldBe 0
    }

    @Test
    fun `Should insert a single row for the worst score`()
    {
        val pt = insertRelevantParticipant()
        insertDartzeeRoundResult(pt, success = false, score = -75)
        insertDartzeeRoundResult(pt, success = false, score = -100)
        insertDartzeeRoundResult(pt, success = false, score = -50)

        factoryAchievement().populateForConversion(emptyList())

        getAchievementCount() shouldBe 1

        val a = retrieveAchievement()
        a.achievementType shouldBe AchievementType.DARTZEE_HALVED
        a.achievementCounter shouldBe 100
        a.gameIdEarned shouldBe pt.gameId
        a.playerId shouldBe pt.playerId
    }

    @Test
    fun `Should tiebreak on the date of the round result`()
    {
        val pt = insertRelevantParticipant()
        insertDartzeeRoundResult(pt, success = false, score = -100, dtCreation = Timestamp(1000))
        insertDartzeeRoundResult(pt, success = false, score = -100, dtCreation = Timestamp(500))
        insertDartzeeRoundResult(pt, success = false, score = -100, dtCreation = Timestamp(1500))

        factoryAchievement().populateForConversion(emptyList())

        getAchievementCount() shouldBe 1

        val a = retrieveAchievement()
        a.dtAchieved shouldBe Timestamp(500)
    }
}