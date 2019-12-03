package burlton.dartzee.code.screen.dartzee

import burlton.dartzee.code.`object`.Dart
import burlton.dartzee.code.`object`.DartboardSegment
import burlton.dartzee.code.dartzee.DartzeeRuleDto
import burlton.dartzee.code.db.DartzeeRoundResultEntity
import burlton.dartzee.code.utils.getAllPossibleSegments
import burlton.desktopcore.code.util.setFontSize
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JPanel

class DartzeeRuleSummaryPanel(parent: IDartzeeCarouselListener, dtos: List<DartzeeRuleDto>): JPanel()
{
    val carousel = DartzeeRuleCarousel(parent, dtos)
    val lblHighScore = JLabel("High Score")
    val panelHighScore = JPanel()

    init
    {
        layout = BorderLayout(0, 0)
        preferredSize = Dimension(150, 120)

        lblHighScore.setFontSize(36)

        panelHighScore.add(lblHighScore)
        add(panelHighScore)
    }

    fun update(results: List<DartzeeRoundResultEntity>, darts: List<Dart>, currentScore: Int, roundNumber: Int)
    {
        if (roundNumber == 1)
        {
            swapInComponentIfNecessary(panelHighScore)
        }
        else
        {
            swapInComponentIfNecessary(carousel)
            carousel.update(results, darts, currentScore)
        }
    }

    private fun swapInComponentIfNecessary(c: Component)
    {
        if (components.contains(c))
        {
            return
        }

        removeAll()

        add(c)
        validate()
        repaint()
    }

    fun getValidSegments(): List<DartboardSegment> =
        when
        {
            components.contains(panelHighScore) -> getAllPossibleSegments()
            else -> carousel.getValidSegments()
        }

    fun gameFinished()
    {
        swapInComponentIfNecessary(carousel)
        carousel.gameFinished()
    }
}
