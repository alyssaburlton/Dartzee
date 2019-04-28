package burlton.dartzee.test.achievements

import burlton.dartzee.code.achievements.AbstractAchievementGamesWon
import burlton.dartzee.code.db.AchievementEntity
import burlton.dartzee.code.db.GameEntity
import burlton.dartzee.code.db.PlayerEntity
import burlton.dartzee.test.helper.*
import io.kotlintest.shouldBe
import org.junit.Test
import java.sql.Timestamp

abstract class TestAbstractAchievementGamesWon<E: AbstractAchievementGamesWon>: AbstractAchievementTest<E>()
{
    override fun beforeEachTest()
    {
        wipeTable("Achievement")
        wipeTable("Game")
        wipeTable("Participant")
        wipeTable("Player")
    }

    private fun insertRelevantGame(): GameEntity
    {
        return insertGame(gameType = factoryAchievement().gameType)
    }


    override fun setUpAchievementRowForPlayer(p: PlayerEntity)
    {
        val game = insertRelevantGame()

        insertParticipant(gameId = game.rowId, playerId = p.rowId, finishingPosition = 1)
    }

    @Test
    fun `Should ignore games of the wrong type`()
    {
        val alice = insertPlayer(name = "Alice")

        val game = insertRelevantGame()
        val golfGame = insertGame(gameType = 500)

        insertParticipant(gameId = game.rowId, playerId = alice.rowId, finishingPosition = 1, dtFinished = Timestamp(1000))
        insertParticipant(gameId = golfGame.rowId, playerId = alice.rowId, finishingPosition = 1, dtFinished = Timestamp(2000))

        factoryAchievement().populateForConversion("")

        getCountFromTable("Achievement") shouldBe 1
        val achievementRow = AchievementEntity().retrieveEntities("")[0]
        achievementRow.playerId shouldBe alice.rowId
        achievementRow.achievementCounter shouldBe 1
        achievementRow.dtLastUpdate shouldBe Timestamp(1000)
    }

    @Test
    fun `Should ignore participants who did not come 1st`()
    {
        val alice = insertPlayer(name = "Alice")
        val game = insertRelevantGame()
        insertParticipant(gameId = game.rowId, playerId = alice.rowId, finishingPosition = 2)

        factoryAchievement().populateForConversion("")

        getCountFromTable("Achievement") shouldBe 0
    }

    @Test
    fun `Should group by player, and take their latest finish date as DtLastUpdate`()
    {
        val alice = insertPlayer(name = "Alice")
        val bob = insertPlayer(name = "Bob")

        val game = insertRelevantGame()

        insertParticipant(gameId = game.rowId, playerId = alice.rowId, finishingPosition = 1, dtFinished = Timestamp(500))
        insertParticipant(gameId = game.rowId, playerId = alice.rowId, finishingPosition = 1, dtFinished = Timestamp(1500))
        insertParticipant(gameId = game.rowId, playerId = alice.rowId, finishingPosition = 1, dtFinished = Timestamp(1000))

        insertParticipant(gameId = game.rowId, playerId = bob.rowId, finishingPosition = 1, dtFinished = Timestamp(2000))
        insertParticipant(gameId = game.rowId, playerId = bob.rowId, finishingPosition = 1, dtFinished = Timestamp(1000))

        factoryAchievement().populateForConversion("")

        getCountFromTable("Achievement") shouldBe 2
        val achievementRows = AchievementEntity().retrieveEntities("")
        val aliceRow = achievementRows.find{ it.playerId == alice.rowId }!!
        aliceRow.achievementCounter shouldBe 3
        aliceRow.dtLastUpdate shouldBe Timestamp(1500)
        aliceRow.gameIdEarned shouldBe ""
        aliceRow.achievementDetail shouldBe ""

        val bobRow = achievementRows.find{ it.playerId == bob.rowId }!!
        bobRow.achievementCounter shouldBe 2
        bobRow.dtLastUpdate shouldBe Timestamp(2000)
        bobRow.gameIdEarned shouldBe ""
        bobRow.achievementDetail shouldBe ""
    }
}