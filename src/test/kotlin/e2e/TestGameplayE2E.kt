package e2e

import dartzee.`object`.Dart
import dartzee.achievements.*
import dartzee.dartzee.DartzeeCalculator
import dartzee.db.DartzeeRoundResultEntity
import dartzee.game.ClockType
import dartzee.game.GameType
import dartzee.game.RoundTheClockConfig
import dartzee.helper.*
import dartzee.utils.InjectedThings
import dartzee.utils.PREFERENCES_INT_AI_SPEED
import dartzee.utils.PreferenceUtil
import dartzee.utils.insertDartzeeRules
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.shouldBe
import org.junit.Test

class TestGameplayE2E: AbstractRegistryTest()
{
    override fun getPreferencesAffected() = listOf(PREFERENCES_INT_AI_SPEED)

    override fun beforeEachTest()
    {
        super.beforeEachTest()
        PreferenceUtil.saveInt(PREFERENCES_INT_AI_SPEED, 100)
    }

    @Test
    fun `E2E - Dartzee`()
    {
        InjectedThings.dartzeeCalculator = DartzeeCalculator()

        val game = insertGame(gameType = GameType.DARTZEE)

        val model = beastDartsModel()
        val player = insertPlayer(model = model)

        val rules = listOf(scoreEighteens, allTwenties)
        insertDartzeeRules(game, rules)

        val (panel, listener) = setUpGamePanel(game)
        panel.startNewGame(listOf(player))
        awaitGameFinish(game)

        val expectedRounds = listOf(
                listOf(Dart(20, 3), Dart(20, 3), Dart(20, 3)), //Scoring round
                listOf(Dart(20, 1), Dart(20, 1), Dart(20, 1)), //All Twenties
                listOf(Dart(18, 1), Dart(18, 1), Dart(25, 2)) //Score Eighteens
        )

        verifyState(panel, listener, expectedRounds, finalScore = 276)

        val participantId = retrieveParticipant().rowId

        val results = DartzeeRoundResultEntity().retrieveEntities().sortedBy { it.roundNumber }
        val roundOne = results.first()
        roundOne.success shouldBe true
        roundOne.ruleNumber shouldBe 2
        roundOne.score shouldBe 60
        roundOne.participantId shouldBe participantId

        val roundTwo = results[1]
        roundTwo.success shouldBe true
        roundTwo.ruleNumber shouldBe 1
        roundTwo.score shouldBe 36
        roundTwo.participantId shouldBe participantId
    }

    @Test
    fun `E2E - Golf`()
    {
        val game = insertGame(gameType = GameType.GOLF, gameParams = "18")

        val model = beastDartsModel()
        val player = insertPlayer(model = model)

        val (panel, listener) = setUpGamePanel(game)
        panel.startNewGame(listOf(player))
        awaitGameFinish(game)

        val expectedDarts = (1..18).map { listOf(Dart(it, 2)) }
        verifyState(panel, listener, expectedDarts, finalScore = 18, expectedScorerRows = 20)

        val expectedAchievementRows = (1..18).map { AchievementSummary(ACHIEVEMENT_REF_GOLF_COURSE_MASTER, -1, game.rowId, "$it") } +
                AchievementSummary(ACHIEVEMENT_REF_GOLF_BEST_GAME, 18, game.rowId)

        retrieveAchievementsForPlayer(player.rowId).shouldContainExactlyInAnyOrder(expectedAchievementRows)
    }

    @Test
    fun `E2E - RTC`()
    {
        val game = insertGame(gameType = GameType.ROUND_THE_CLOCK, gameParams = RoundTheClockConfig(ClockType.Standard, true).toJson())

        val model = beastDartsModel()
        val player = insertPlayer(model = model)

        val (panel, listener) = setUpGamePanel(game)
        panel.startNewGame(listOf(player))
        awaitGameFinish(game)

        val expectedDarts = (1..20).map { Dart(it, 1) }.chunked(4)
        verifyState(panel, listener, expectedDarts, 20, scoreSuffix = " Darts")

        retrieveAchievementsForPlayer(player.rowId).shouldContainExactlyInAnyOrder(
                AchievementSummary(ACHIEVEMENT_REF_CLOCK_BEST_GAME, 20, game.rowId),
                AchievementSummary(ACHIEVEMENT_REF_CLOCK_BEST_STREAK, 20, game.rowId),
                AchievementSummary(ACHIEVEMENT_REF_CLOCK_BRUCEY_BONUSES, 5, "")
        )
    }
}