package dartzee.bean

import com.github.alexburlton.swingtest.shouldMatchImage
import dartzee.helper.AbstractTest
import dartzee.helper.insertPlayer
import dartzee.helper.insertPlayerImage
import dartzee.screen.game.makeTeam
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class TestParticipantAvatar : AbstractTest()
{
    @Test
    @Tag("screenshot")
    fun `Should default to the split avatar, and update accordingly based on round number`()
    {
        val playerOneImage = insertPlayerImage(resource = "yoshi")
        val playerOne = insertPlayer(playerImageId = playerOneImage.rowId)

        val playerTwoImage = insertPlayerImage(resource = "Bean")
        val playerTwo = insertPlayer(playerImageId = playerTwoImage.rowId)
        val team = makeTeam(playerOne, playerTwo)

        val avatar = ParticipantAvatar(team)
        avatar.shouldMatchImage("unselected")

        avatar.setSelected(true, 1)
        avatar.shouldMatchImage("player-one")

        avatar.setSelected(true, 2)
        avatar.shouldMatchImage("player-two")

        avatar.setSelected(true, 3)
        avatar.shouldMatchImage("player-one")
    }
}